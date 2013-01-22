package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.propsrc.PropertySource;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import com.github.dirkraft.propslive.util.ComboLock;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides atomic configuration operations per {@link Props} which this class implements. as well as enables
 * {@link PropListener}s to subscribe to change events of certain properties.
 *
 * <hr/>
 *
 * Subscribe a listener by use of chaining in the {@link #to(PropListener)} method. e.g.
 *
 * <pre>
 * dynamicProperties.to(myListener).getString("project.whatever.propkey")
 * </pre>
 *
 * Then when anything anywhere calls a setter on the same DynamicProperties instance that alters the property value
 * "project.whatever.propkey", 'myListener' will be notified of the change through the registered
 * {@link PropListener#reload(PropChange)}. Many different listeners can be registered this way.
 * <p/>
 * Note that chaining on a set has potential to <strong>immediately</strong> trigger a reload, e.g.
 * <pre>
 * dynamicProperties.to(myListener).setString("project.whatever.propkey", "red_theme")
 * </pre>
 * if "red_theme" is different that the prior value will trigger reload on 'myListener'.
 *
 * <hr/>
 *
 * Concurrent writing of the same property will result in a {@link PropLockingException} at this time. Thoughts:
 * <ul>
 *     <li>intersecting prop sets being written concurrently (currently might cause PropLockingException)</li>
 *     <li>intersecting prop sets, intersection resolution: merge? ensure non-overlapping updates (sequential queue)</li>
 *     <li>queue prop updates so that overlap cannot occur</li>
 *     <li>queue prop set updates so that even if singular updates are coming in, prop sets will not be able to contend
 *         with each other</li>
 * </ul>
 * If so much of the above massive scope creep ever becomes supported, this could become a lot more than just a dynamic
 * configuration framework. Thoughts...
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicProps implements PropsSets {

    private static Logger logger = LoggerFactory.getLogger(DynamicProps.class);

    /**
     * Set by {@link #to(PropListener)} and ready by {@link #proxy}
     */
    private static final ThreadLocal<PropListener<?>> listener = new ThreadLocal<>();

    /** Keys are actually String prop keys or {@link PropSet}s */
    private final ConcurrentHashMap<Object, ReadWriteLock> propLocks = new ConcurrentHashMap<>();
    /** Keys are actually String prop keys or {@link PropSet}s */
    private final ConcurrentHashMap<Object, Set<PropListener<?>>> propsToListeners = new ConcurrentHashMap<>();

    /**
     * As a field, instead of having DynamicProps extend PropsSetsImpl, so that I can make sure that no methods are
     * correctly overridden. If DynamicProps extend PropsSetsImpl, it's possible to miss overriding a method to
     * delegate to the proxy.
     */
    private final PropsSets impl;

    /**
     * All {@link Props} accesses go through here. All accesses will register the listener in {@link #listener} to the
     * interested property (String).
     */
    private final Props proxy = (Props) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Props.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock lock = null;
            boolean lockAcquired = false;

            Object ret;

            try {
                String propKey = (String) args[0];

                boolean get = method.getName().startsWith("get");
                boolean set = method.getName().startsWith("set");

                PropListener<?> propListener;
                // Get or set is fine; whatever. Both can subscribe a listener.
                if ((get || set) && (propListener = listener.get()) != null) {
                    registerListener(propKey, propListener);
                }

                // Effectively block reads if there is currently a write, or causes concurrent writes to throw an
                // exception; concurrent changing of the same property is not supported.
                if (get) {
                    lock = readLock(propKey);
                    lock.lock();
                    lockAcquired = true;
                    ret = method.invoke(impl, args);

                } else if (set) {
                    lock = writeLock(propKey);
                    lockAcquired = lock.tryLock();

                    if (!lockAcquired) {
                        throw new PropLockingException("Failed to acquire write lock for prop " + propKey + " as it " +
                                "was already locked.");
                    }

                    Method getter = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    Object previous = getter.invoke(impl, propKey);
                    Object newVal = args[1];
                    if (!ObjectUtils.equals(previous, newVal)) {
                        notifyListeners(propKey, previous, newVal);
                    }
                    ret = method.invoke(impl, args);

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                listener.remove(); // reset affected listener
                if (lockAcquired) {
                    lock.unlock(); // reset any owned lock
                }
            }

            return ret;
        }
    });

    /**
     * The {@link PropsSets} version of {@link #proxy}. All PropsSets accesses go through here. All accesses will
     * register the listener in {@link #listener} to the interested {@link PropSet}.
     */
    private final PropsSets setProxy = (PropsSets) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PropsSets.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock lock = null;
            boolean lockAcquired = false;

            Object ret;

            try {
                final PropSet<?> propSet = (PropSet<?>) args[0];

                boolean get = method.getName().startsWith("get");
                boolean set = method.getName().startsWith("set");

                PropListener<?> propListener;
                // Get or set is fine; whatever. Both can subscribe a listener.
                if ((get || set) && (propListener = listener.get()) != null) {
                    registerListener(propSet, propListener);
                }

                // Effectively block reads if there is currently a write, or causes concurrent writes to throw an
                // exception; concurrent changing of the same property is not supported. Note that in this version,
                // multiple locks must be acquired for the PropSet get/set to be atomic.
                if (get) {
                    lock = readLock(propSet);
                    lock.lock();
                    lockAcquired = true;

                    // construct view of props containing only those of interest to the prop set
                    ret = method.invoke(new PropsImpl(new PropertySourceMap() {{
                        for (String propKey : propSet.propKeys()) {
                            setProp(propKey, impl.getString(propKey));
                        }
                    }}), propSet);

                } else if (set) {
                    lock = writeLock(propSet);
                    lockAcquired = lock.tryLock();

                    if (!lockAcquired) {
                        throw new PropLockingException("Failed to acquire write lock for prop set " + propSet + " as " +
                                "it was already locked.");
                    }

                    Method getter = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    Object previousVals = getter.invoke(impl, propSet);
                    ret = method.invoke(impl, propSet);
                    Object newVals = getter.invoke(impl, propSet);
                    if (!ObjectUtils.equals(previousVals, newVals)) {
                        notifyListeners(propSet, previousVals, newVals);
                    }

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                listener.remove();
                if (lockAcquired) {
                    lock.unlock();
                }
            }

            return ret;
        }
    });

    /**
     * Backed by that of {@link PropsSetsImpl#PropsSetsImpl()}
     */
    public DynamicProps() {
        impl = new PropsSetsImpl();
    }

    /**
     * Backed by arbitrary PropertySource
     *
     * @param source of props
     */
    public DynamicProps(PropertySource source) {
        impl = new PropsSetsImpl(source);
    }

    /**
     * @param dynPropsListener who should register as a listener on the following property, e.g.
     *                         <code>dynamicProperties.to(myListener).get("flag.enabled")</code> will be registered
     *                         as a listener of "flag.enabled"
     * @return this for chaining
     */
    public PropsSets to(final PropListener<?> dynPropsListener) {
        listener.set(dynPropsListener);
        return this;
    }

    private Lock readLock(String propKey) {
        return getLock(propKey).readLock();
    }

    private Lock writeLock(String propKey) {
        return getLock(propKey).writeLock();
    }

    private ReadWriteLock getLock(String propKey) {
        ReadWriteLock lock = propLocks.get(propKey);
        if (lock == null) {
            propLocks.putIfAbsent(propKey, new ReentrantReadWriteLock());
            lock = propLocks.get(propKey);
        }
        return lock;
    }

    private Lock readLock(PropSet<?> propSet) {
        return getLock(propSet).readLock();
    }

    private Lock writeLock(PropSet<?> propSet) {
        return getLock(propSet).writeLock();
    }

    private ReadWriteLock getLock(PropSet<?> propSet) {
        ReadWriteLock lock = propLocks.get(propSet);
        if (lock == null) {
            assert lock instanceof ComboLock;

            // A ComboLock is made up of a bunch of individual property ReadWriteLocks
            List<ReadWriteLock> readWriteLocks = new ArrayList<>(propSet.propKeys().size());
            for (String propKey : propSet.propKeys()) {
                readWriteLocks.add(getLock(propKey));
            }

            propLocks.putIfAbsent(propSet, new ComboLock(readWriteLocks));
            lock = propLocks.get(propSet);
        }
        return lock;
    }

    private void registerListener(Object propKeyOrSet, PropListener<?> listener) {
        Set<PropListener<?>> listenerSet = propsToListeners.get(propKeyOrSet);
        if (listenerSet == null) {
            propsToListeners.putIfAbsent(propKeyOrSet, Collections.newSetFromMap(new ConcurrentHashMap<PropListener<?>, Boolean>()));
            listenerSet = propsToListeners.get(propKeyOrSet);
        }
        listenerSet.add(listener);
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyListeners(Object propKeyOrSet, T before, T after) {
        Set<PropListener<?>> propListeners = propsToListeners.get(propKeyOrSet);
        if (propListeners != null) {
            for (PropListener<?> propListener : propListeners) {
                PropChange<T> propChange = new PropChange<>(before, after);
                try {
                    ((PropListener<T>) propListener).reload(propChange);
                } catch (Throwable t) {
                    logger.error("Exception reloading listener " + propListener + " with change " + propChange, t);
                }
            }
        }
    }

    @Override
    public String description() {
        return impl.description();
    }

    /* ***** Props interface impl delegates to proxy ***** */

    @Override
    public Boolean getBool(String key) {
        return proxy.getBool(key);
    }

    @Override
    public Boolean getBool(String key, Boolean def) {
        return proxy.getBool(key, def);
    }

    @Override
    public Byte getByte(String key) {
        return proxy.getByte(key);
    }

    @Override
    public Byte getByte(String key, Byte def) {
        return proxy.getByte(key, def);
    }

    @Override
    public Short getShort(String key) {
        return proxy.getShort(key);
    }

    @Override
    public Short getShort(String key, Short def) {
        return proxy.getShort(key, def);
    }

    @Override
    public Integer getInt(String key) {
        return proxy.getInt(key);
    }

    @Override
    public Integer getInt(String key, Integer def) {
        return proxy.getInt(key, def);
    }

    @Override
    public Long getLong(String key) {
        return proxy.getLong(key);
    }

    @Override
    public Long getLong(String key, Long def) {
        return proxy.getLong(key, def);
    }

    @Override
    public Float getFloat(String key) {
        return proxy.getFloat(key);
    }

    @Override
    public Float getFloat(String key, Float def) {
        return proxy.getFloat(key, def);
    }

    @Override
    public Double getDouble(String key) {
        return proxy.getDouble(key);
    }

    @Override
    public Double getDouble(String key, Double def) {
        return proxy.getDouble(key, def);
    }

    @Override
    public Character getChar(String key) {
        return proxy.getChar(key);
    }

    @Override
    public Character getChar(String key, Character def) {
        return proxy.getChar(key, def);
    }

    @Override
    public String getString(String key) {
        return proxy.getString(key);
    }

    @Override
    public String getString(String key, String def) {
        return proxy.getString(key, def);
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumCls) {
        return proxy.getEnum(key, enumCls);
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls) {
        return proxy.getEnum(key, def, enumCls);
    }

    @Override
    public void setBool(String key, Boolean value) {
        proxy.setBool(key, value);
    }

    @Override
    public void setByte(String key, Byte value) {
        proxy.setByte(key, value);
    }

    @Override
    public void setShort(String key, Short value) {
        proxy.setShort(key, value);
    }

    @Override
    public void setInt(String key, Integer value) {
        proxy.setInt(key, value);
    }

    @Override
    public void setLong(String key, Long value) {
        proxy.setLong(key, value);
    }

    @Override
    public void setFloat(String key, Float value) {
        proxy.setFloat(key, value);
    }

    @Override
    public void setDouble(String key, Double value) {
        proxy.setDouble(key, value);
    }

    @Override
    public void setCharacter(String key, Character value) {
        proxy.setCharacter(key, value);
    }

    @Override
    public void setString(String key, String value) {
        proxy.setString(key, value);
    }

    @Override
    public <T extends Enum<T>> void setEnum(String key, T value) {
        proxy.setEnum(key, value);
    }

    @Override
    public <VALUES> VALUES getPropSet(PropSet<VALUES> propSet) {
        return setProxy.getPropSet(propSet);
    }

    @Override
    public <VALUES> void setPropSet(PropSet<VALUES> propSet) {
        setProxy.setPropSet(propSet);
    }

}
