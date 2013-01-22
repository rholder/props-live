package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.PropSetKeys;
import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsSets;
import com.github.dirkraft.propslive.PropsSetsImpl;
import com.github.dirkraft.propslive.propsrc.PropertySource;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides atomic configuration operations per {@link Props} which this class implements. as well as enables
 * {@link DynamicPropListener}s to subscribe to change events of certain properties.
 * <p/>
 * Subscribe a listener by use of chaining in the {@link #to(DynamicPropListener)} method. e.g.
 * <code>
 * dynamicProperties.to(myListener).getString("project.whatever.propkey")
 * </code>
 * Then when anything anywhere calls a setter on the same DynamicProperties instance that alters the property value
 * "project.whatever.propkey", 'myListener' will be notified of the change through the registered
 * {@link DynamicPropListener#reload(PropChange)}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicProps implements Props {

    private static Logger logger = LoggerFactory.getLogger(DynamicProps.class);

    /**
     * Set by {@link #to(DynamicPropListener)} and ready by {@link #proxy}
     */
    private static final ThreadLocal<DynamicPropListener<?>> listener = new ThreadLocal<>();

    private final ConcurrentHashMap<String, ReadWriteLock> propLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<DynamicPropListener<?>>> propsToListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PropSetKeys> propsToPropSets = new ConcurrentHashMap<>();

    /**
     * As a field, instead of having DynamicProps extend PropsSetsImpl, so that I can make sure that no methods are
     * correctly overridden. If DynamicProps extend PropsSetsImpl, it's possible to miss overriding a method to
     * delegate to the proxy.
     */
    private final PropsSets impl;

    /**
     * All prop accesses go through here. All accesses will register the listener in {@link #listener} to the interested
     * property (String) or {@link PropSetKeys} (as the argument appears in {@link PropsSets} methods)
     */
    private final PropsSets proxy = (PropsSets) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PropsSets.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                String propKey = (String) args[0];

                boolean get = method.getName().startsWith("get");
                boolean set = method.getName().startsWith("set");

                if (set) {
                    Method getter = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    Object previous = getter.invoke(impl, propKey);
                    Object newVal = args[1];
                    if (!ObjectUtils.equals(previous, newVal)) {
                        notifyListeners(propKey, previous, newVal);
                    }
                }

                DynamicPropListener<?> dynamicPropListener;
                // Get or set is fine; whatever. Both can subscribe a listener.
                if ((get || set) && (dynamicPropListener = listener.get()) != null) {
                    registerListener(propKey, dynamicPropListener);
                }

                Lock lock = null;
                boolean lockAcquired = false;
                try {
                    // Effectively block reads if there is currently a write. Also causes concurrent writes to throw an
                    // exception; concurrent changing of the same property smells like a bug.
                    if (get) {
                        lock = readLock(propKey);
                        lock.lock();
                        lockAcquired = true;
                    } else if (set) {
                        lock = writeLock(propKey);
                        lockAcquired = lock.tryLock();
                        if (!lockAcquired) {
                            throw new RuntimeException("Failed to acquire write lock for prop " + propKey + " as it was " +
                                    "already locked. This assumes that the cause of concurrent writes to the same property " +
                                    "is a bug.");
                        }
                    }
                } finally {
                    if (lockAcquired) {
                        lock.unlock();
                    }
                }

                return method.invoke(impl, args);

            } finally {
                listener.remove(); // reset affected listener
            }
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
     * @return a proxy of {@link PropsSets} that will register a {@link DynamicPropListener} against the next
     *         property access (get or set) through the proxy. Note that this will only work ONCE, against "the next
     *         property access" and so should always be chained as the example in the 'dynPropsListener' parameter
     *         documentation.
     */
    @SuppressWarnings("unchecked")
    public PropsSets to(final DynamicPropListener<?> dynPropsListener) {
        listener.set(dynPropsListener);
        return proxy;
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

    private void registerListener(String propKey, DynamicPropListener<?> listener) {
        Set<DynamicPropListener<?>> listenerSet = propsToListeners.get(propKey);
        if (listenerSet == null) {
            propsToListeners.putIfAbsent(propKey, Collections.newSetFromMap(new ConcurrentHashMap<DynamicPropListener<?>, Boolean>()));
            listenerSet = propsToListeners.get(propKey);
        }
        listenerSet.add(listener);
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyListeners(String propKey, T before, T after) {
        Set<DynamicPropListener<?>> dynamicPropListeners = propsToListeners.get(propKey);
        if (dynamicPropListeners != null) {
            for (DynamicPropListener<?> dynamicPropListener : dynamicPropListeners) {
                ((DynamicPropListener<T>) dynamicPropListener).reload(new PropChange<>(before, after));
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

}
