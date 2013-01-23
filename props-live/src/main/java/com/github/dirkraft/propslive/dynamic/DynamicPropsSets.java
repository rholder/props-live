package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.dynamic.listen.PropListener;
import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.propsrc.PropertySource;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import com.github.dirkraft.propslive.set.PropSet;
import com.github.dirkraft.propslive.set.PropsSets;
import com.github.dirkraft.propslive.set.PropsSetsImpl;
import com.github.dirkraft.propslive.util.ComboLock;
import com.github.dirkraft.propslive.view.LayeredPropertySource;
import org.apache.commons.lang3.ObjectUtils;

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

/**
 * Extension of {@link DynamicProps} with support for methods of the {@link com.github.dirkraft.propslive.set.PropsSets} interface, that is, support for
 * atomically getting and setting sets of properties, and also subscribing to changes on any of the constituent props
 * of some {@link PropSet}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicPropsSets extends DynamicProps<PropsSets> implements com.github.dirkraft.propslive.set.PropsSets {

    /**
     * Set by {@link #to(PropSetListener)} and read by {@link #setProxy}
     */
    static final ThreadLocal<PropSetListener<?>> setListener = new ThreadLocal<>();

    /** Keys are {@link PropSet}s */
    private final ConcurrentHashMap<PropSet<?>, ComboLock> propSetLocks = new ConcurrentHashMap<>();
    /**
     * Keys are String prop keys. Listeners on {@link PropSet}s are registered for every property in
     * {@link PropSet#propKeys()}
     */
    private final ConcurrentHashMap<String, Set<PropSetListener<?>>> propsToSetListeners = new ConcurrentHashMap<>();

    /**
     * The {@link com.github.dirkraft.propslive.set.PropsSets} version of {@link #proxy}. All PropsSets accesses go through here. All accesses will
     * register the listener in {@link #listener} to the interested {@link PropSet}.
     */
    private final com.github.dirkraft.propslive.set.PropsSets setProxy = (com.github.dirkraft.propslive.set.PropsSets) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{com.github.dirkraft.propslive.set.PropsSets.class}, new InvocationHandler() {

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
                    assert method.getName().matches("(get|set)Vals");
                    // Get or set is fine; whatever. Both can subscribe a listener.
                    if ((propSetListener = DynamicPropsSets.setListener.get()) != null) {
                        registerListener(propSet, propSetListener);
                    }
                }

                // Effectively block reads if there is currently a write, or causes concurrent writes to throw an
                // exception; concurrent changing of the same property is not supported. Note that in this version,
                // multiple locks must be acquired for the PropSet get/set to be atomic.
                if (get) {
                    assert method.getName().matches("getVals");
                    lock = readLock(propSet);
                    lock.lock();
                    lockAcquired = true;
                    ret = method.invoke(impl, propSet);

                } else if (set) {
                    assert method.getName().matches("setVals");
                    lock = writeLock(propSet);
                    lockAcquired = lock.tryLock();

                    if (!lockAcquired) {
                        throw new PropLockingException("Failed to acquire write lock for prop set " + propSet + " as " +
                                "it was already locked.");
                    }

                    Map<String, String> beforeVals = propVals(propSet.propKeys());
                    // (atomically) does the property updates as dictated by the PropSet impl
                    ret = method.invoke(impl, propSet);
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

                    Method propsSets_getPropSet = com.github.dirkraft.propslive.set.PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    for (PropSetListener<?> affectedPropSetListener : affectedPropSetListeners(changedProps.keySet())) {
                        Object beforePojo = propsSets_getPropSet.invoke(beforeView, affectedPropSetListener.propSet());
                        Object afterPojo = propsSets_getPropSet.invoke(afterView, affectedPropSetListener.propSet());
                        notifyListener(affectedPropSetListener, new PropChange<>(beforePojo, afterPojo));
                    }

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                DynamicPropsSets.setListener.remove();
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
                Set<PropSetListener<?>> setListeners = propsToSetListeners.get(changedProp);
                if (setListeners != null) {
                    affectedListeners.addAll(setListeners);
                }
            }
            return affectedListeners;
        }

        /** Just to specifically locate a @SuppressWarnings("unchecked") */
        @SuppressWarnings("unchecked")
        private void notifyListener(PropSetListener<?> propSetListener, PropChange<Object> propChange) {
            DynamicPropsSets.this.notifyListener((PropListener<Object>) propSetListener, propChange);
        }

        private Lock readLock(PropSet<?> propSet) {
            return getLock(propSet).readLock();
        }

        private Lock writeLock(PropSet<?> propSet) {
            return getLock(propSet).writeLock();
        }

    });

    public DynamicPropsSets() {
        super(new PropsSetsImpl());
    }

    public DynamicPropsSets(PropertySource source) {
        super(new PropsSetsImpl(source));
    }

    /**
     * @param propSetListener who should register as a set listener on the properties of the corresponding propset
     *                        ({@link PropSetListener#propSet()} {@link PropSet#propKeys()})
     * @return this for chaining
     */
    public com.github.dirkraft.propslive.set.PropsSets to(final PropSetListener<?> propSetListener) {
        DynamicPropsSets.setListener.set(propSetListener);
        return this;
    }

    private ReadWriteLock getLock(PropSet<?> propSet) {
        ComboLock lock = propSetLocks.get(propSet);
        if (lock == null) {
            // A ComboLock is made up of a bunch of individual property ReadWriteLocks
            List<ReadWriteLock> readWriteLocks = new ArrayList<>(propSet.propKeys().size());
            for (String propKey : propSet.propKeys()) {
                readWriteLocks.add(super.getLock(propKey));
            }

            propSetLocks.putIfAbsent(propSet, new ComboLock(readWriteLocks));
            lock = propSetLocks.get(propSet);
        }
        return lock;
    }

    @Override
    public <VALUES> VALUES getVals(PropSet<VALUES> propSet) {
        return setProxy.getVals(propSet);
    }

    @Override
    public void setVals(PropSet<?> propSet) {
        setProxy.setVals(propSet);
    }

}
