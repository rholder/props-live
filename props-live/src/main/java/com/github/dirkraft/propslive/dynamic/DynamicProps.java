package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.listen.PropListener;
import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.propsrc.PropertySource;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import com.github.dirkraft.propslive.util.ComboLock;
import com.github.dirkraft.propslive.view.LayeredPropertySource;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Set by {@link #to(PropListener)} and read by {@link #proxy}
     */
    private static final ThreadLocal<PropListener<?>> listener = new ThreadLocal<>();
    /**
     * Set by {@link }
     */
    private static final ThreadLocal<PropSetListener<?>> setListener = new ThreadLocal<>();
    /**
     * For {@link PropsSets} method access to properties to restrict them to only those they declare in their
     * {@link PropSet#propKeys()}.
     */
    private static final ThreadLocal<Set<String>> propRestrictions = new ThreadLocal<>();

    /** Keys are String prop keys */
    private final ConcurrentHashMap<String, ReadWriteLock> propLocks = new ConcurrentHashMap<>();
    /** Keys are {@link PropSet}s */
    private final ConcurrentHashMap<PropSet<?>, ComboLock> propSetLocks = new ConcurrentHashMap<>();
    /**
     * Keys are String prop keys.
     */
    private final ConcurrentHashMap<String, Set<PropListener<?>>> propsToSingleListeners = new ConcurrentHashMap<>();
    /**
     * Keys are String prop keys. Listeners on {@link PropSet}s are registered for every property in
     * {@link PropSet#propKeys()}
     */
    private final ConcurrentHashMap<String, Set<PropSetListener<?>>> propsToSetListeners = new ConcurrentHashMap<>();

    /**
     * As a field, instead of having DynamicProps extend PropsSetsImpl, so that I can make sure that no methods are
     * correctly overridden. If DynamicProps extend PropsSetsImpl, it's possible to miss overriding a method to
     * delegate to the proxy.
     */
    private final Props impl;

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
                        notifyListeners(propKey, new PropChange<>(previous, newVal));
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

        private void registerListener(String propKey, PropListener<?> listener) {
            Set<PropListener<?>> listenerSet = propsToSingleListeners.get(propKey);
            if (listenerSet == null) {
                propsToSingleListeners.putIfAbsent(propKey, Collections.newSetFromMap(new ConcurrentHashMap<PropListener<?>, Boolean>()));
                listenerSet = propsToSingleListeners.get(propKey);
            }
            listenerSet.add(listener);
        }
    });

    /**
     * The {@link PropsSets} version of {@link #proxy}. All PropsSets accesses go through here. All accesses will
     * register the listener in {@link #listener} to the interested {@link PropSet}.
     */
    private final PropsSets setProxy = (PropsSets) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PropsSets.class}, new InvocationHandler() {

        /**
         * Restricts access to {@link DynamicProps#impl} by that set in {@link DynamicProps#propRestrictions}.
         */
        final Props restrictedProxy = (Props) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Props.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().startsWith("get") || method.getName().startsWith("set")) {
                    assert method.getName().matches("(get|set)PropSet");
                    String propKey = (String) args[0];
                    if (!propRestrictions.get().contains(propKey)) {
                        throw new IllegalAccessException("PropSet attempted to access property '" + propKey + "' that " +
                                "was not in its declared PropSet.propKeys(): " + propRestrictions.get());
                    }
                }
                return method.invoke(impl, args);
            }
        });

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock lock = null;
            boolean lockAcquired = false;

            Object ret;

            try {
                @SuppressWarnings("unchecked")
                final PropSet<Object> propSet = (PropSet<Object>) args[0];

                boolean get = method.getName().startsWith("get");
                boolean set = method.getName().startsWith("set");

                PropSetListener<?> propSetListener;
                if (get || set) {
                    assert method.getName().matches("(get|set)PropSet");
                    // Get or set is fine; whatever. Both can subscribe a listener.
                    if ((propSetListener = setListener.get()) != null) {
                        registerListener(propSet, propSetListener);
                    }
                    // Restrict property access to the PropSet to those declared by its PropSet.propKeys()
                    propRestrictions.set(propSet.propKeys());
                }

                // Effectively block reads if there is currently a write, or causes concurrent writes to throw an
                // exception; concurrent changing of the same property is not supported. Note that in this version,
                // multiple locks must be acquired for the PropSet get/set to be atomic.
                if (get) {
                    assert method.getName().matches("getPropSet");
                    lock = readLock(propSet);
                    lock.lock();
                    lockAcquired = true;
                    ret = method.invoke(restrictedProxy, propSet);

                } else if (set) {
                    assert method.getName().matches("setPropSet");
                    lock = writeLock(propSet);
                    lockAcquired = lock.tryLock();

                    if (!lockAcquired) {
                        throw new PropLockingException("Failed to acquire write lock for prop set " + propSet + " as " +
                                "it was already locked.");
                    }

                    // collect the resulting pojo, AND a straight map of values for comparison.
                    Map<String, String> beforeVals = propVals(propSet.propKeys());

                    // (atomically) does the property updates as dictated by the PropSet impl
                    ret = method.invoke(restrictedProxy, propSet);

                    // Again, collect the resulting pojo, and a simple map of values for comparison.
                    Map<String, String> afterVals = propVals(propSet.propKeys());

                    Map<String, PropChange<?>> changedProps = changedProps(beforeVals, afterVals);

                    // First, trigger single prop listeners
                    for (Map.Entry<String, PropChange<?>> changedPropEntry : changedProps.entrySet()) {
                        notifyListeners(changedPropEntry.getKey(), changedPropEntry.getValue());
                    }

                    // Second, trigger all PropSet's that contain any changed prop. They each expect a particular
                    // arbitrary pojo constructed and returned by their PropSet.getVals(Props). So in order to get a
                    // PropChange<POJO>, we need the before and after values of all the properties for each POJO.
                    Props beforeView = new PropsImpl(new LayeredPropertySource(
                            new PropertySourceMap("before prop changes view", beforeVals), impl
                    ));
                    Props afterView = impl; // The current state of properties is the after view.

                    Method propsSets_getPropSet = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    for (PropSetListener<?> affectedPropSetListener : affectedPropSetListeners(changedProps.keySet())) {
                        Object beforePojo = propsSets_getPropSet.invoke(beforeView, affectedPropSetListener.propSet());
                        Object afterPojo = propsSets_getPropSet.invoke(afterView, affectedPropSetListener.propSet());
                        notifyListener((PropListener<Object>) affectedPropSetListener, new PropChange<>(beforePojo, afterPojo));
                    }

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                setListener.remove();
                propRestrictions.remove();
                if (lockAcquired) {
                    lock.unlock();
                }
            }

            return ret;
        }

        private void registerListener(PropSet<?> propSet, PropSetListener<?> listener) {
            for (String propKey : propSet.propKeys()) {
                Set<PropSetListener<?>> listenerSet = propsToSetListeners.get(propKey);
                if (listener == null) {
                    propsToSetListeners.putIfAbsent(propKey, Collections.newSetFromMap(new ConcurrentHashMap<PropSetListener<?>, Boolean>()));
                    listenerSet = propsToSetListeners.get(propKey);
                }
                listenerSet.add(listener);
            }
        }

        private Map<String, String> propVals(Collection<String> propKeys) {
            Map<String, String> vals = new HashMap<>(propKeys.size());
            for (String propKey : propKeys) {
                vals.put(propKey, impl.getString(propKey));
            }
            return vals;
        }

        private Map<String, PropChange<?>> changedProps(Map<String, String> before, Map<String, String> after) {
            Map<String, PropChange<?>> changedProps = new HashMap<>();
            for (String propKey : before.keySet()) {
                Object beforeVal = before.get(propKey);
                Object afterVal = after.get(propKey);
                if (!ObjectUtils.equals(beforeVal, afterVal)) {
                    changedProps.put(propKey, new PropChange<>(beforeVal, afterVal));
                }
            }
            return changedProps;
        }

        private Set<PropSetListener<?>> affectedPropSetListeners(Set<String> changedProps) {
            Set<PropSetListener<?>> affectedListeners = new HashSet<>();
            for (String changedProp : changedProps) {
                affectedListeners.addAll(propsToSetListeners.get(changedProp));
            }
            return affectedListeners;
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
     * @param propListener who should register as a listener on the following property, e.g.
     *                     <code>dynamicProperties.to(myListener).get("flag.enabled")</code> will be registered
     *                     as a listener of "flag.enabled"
     * @return this for chaining
     */
    public Props to(final PropListener<?> propListener) {
        listener.set(propListener);
        return this;
    }

    /**
     * @param propSetListener who should register as a set listener on the properties of the corresponding propset
     *                        ({@link PropSetListener#propSet()} {@link PropSet#propKeys()})
     * @return this for chaining
     */
    public PropsSets to(final PropSetListener<?> propSetListener) {
        setListener.set(propSetListener);
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
        ComboLock lock = propSetLocks.get(propSet);
        if (lock == null) {
            // A ComboLock is made up of a bunch of individual property ReadWriteLocks
            List<ReadWriteLock> readWriteLocks = new ArrayList<>(propSet.propKeys().size());
            for (String propKey : propSet.propKeys()) {
                readWriteLocks.add(getLock(propKey));
            }

            propSetLocks.putIfAbsent(propSet, new ComboLock(readWriteLocks));
            lock = propSetLocks.get(propSet);
        }
        return lock;
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyListeners(String propKey, PropChange<T> propChange) {
        Set<PropListener<?>> propListeners = propsToSingleListeners.get(propKey);
        if (propListeners != null) {
            for (PropListener<?> propListener : propListeners) {
                notifyListener((PropListener<T>) propListener, propChange);
            }
        }
    }

    private <T> void notifyListener(PropListener<T> propListener, PropChange<T> propChange) {
        try {
            propListener.reload(propChange);
        } catch (Throwable t) {
            logger.error("Exception reloading listener " + propListener + " with change " + propChange, t);
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