package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.dynamic.listen.PropListener;
import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.propsrc.view.LayeredPropSource;
import com.github.dirkraft.propslive.set.PropSet;
import com.github.dirkraft.propslive.set.PropsSets;
import com.github.dirkraft.propslive.set.PropsSetsImpl;
import com.github.dirkraft.propslive.util.ComboLock;
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

/**
 * Extension of {@link DynamicProps} with support for methods of the {@link PropsSets} interface, that is, support for
 * atomically getting and setting sets of properties, and also subscribing to changes on any of the constituent props
 * of some {@link PropSet}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicPropsSets extends DynamicProps<PropsSets> implements PropsSets {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPropsSets.class);

    /**
     * Set by {@link #to(PropSetListener)} and read by {@link #setProxy}
     */
    private static final ThreadLocal<PropSetListener<?>> setListener = new ThreadLocal<>();

    /** Keys are {@link PropSet}s */
    private final ConcurrentHashMap<PropSet<?>, ComboLock> propSetLocks = new ConcurrentHashMap<>();
    /**
     * Keys are String prop keys. Listeners on {@link PropSet}s are registered for every property in
     * {@link PropSet#propKeys()}
     */
    private final ConcurrentHashMap<String, Set<PropSetListener<?>>> propsToSetListeners = new ConcurrentHashMap<>();

    /**
     * The {@link PropsSets} version of {@link #proxy}. All PropsSets accesses go through here. All accesses will
     * register the listener in {@link #listener} to the interested {@link PropSet}.
     */
    private final PropsSets setProxy = (PropsSets) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PropsSets.class}, new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock cloneLock = null;

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
                    if ((propSetListener = setListener.get()) != null) {
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
                    cloneLock = DynamicPropsSets.this.cloneLock.readLock();
                    cloneLock.lock();

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
                    assert ret == null; // it's a void method
                    Map<String, String> afterVals = propVals(propSet.propKeys());
                    Map<String, PropChange<?>> changedProps = changedProps(beforeVals, afterVals);

                    // First, trigger all PropSet's that contain any changed prop. They each expect a particular
                    // arbitrary pojo constructed and returned by their PropSet.getVals(Props). So in order to get a
                    // PropChange<POJO>, we need the before and after values of all the properties for each POJO.
                    Props beforeView = new PropsSetsImpl(new LayeredPropSource(
                            new PropSourceMap("before prop changes view", beforeVals), impl
                    ));
                    Props afterView = impl; // The current state of properties is the after view.

                    // Starting at this point, we are careful to attempt to fire every registered listener once.

                    Method propsSets_getPropSet = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    Set<PropSetListener<?>> affectedPropSetListeners = affectedPropSetListeners(changedProps.keySet());
                    for (PropSetListener<?> affectedPropSetListener : affectedPropSetListeners) {
                        Object beforePojo = null, afterPojo = null;
                        try {
                            beforePojo = propsSets_getPropSet.invoke(beforeView, affectedPropSetListener.propSet());
                            afterPojo = propsSets_getPropSet.invoke(afterView, affectedPropSetListener.propSet());
                        } catch (Exception e) {
                            logger.error("Failed to compute PropChange for listener " + affectedPropSetListener.getClass()
                                    + " on prop set of " + affectedPropSetListener.propSet().propKeys(), e);
                        }
                        // The eventual call to listener.reload is already wrapped in a try-catch, so keep this out
                        // of the previous try-catch. If an exception escapes from here, it is a library bug.
                        notifyListener(affectedPropSetListener, new PropChange<>(beforePojo, afterPojo));
                    }

                    // Second, trigger any remaining single prop listeners. PropSetListeners are also registered with
                    // DynamicProps#propsToSingleListeners so that singular property changes will fire correctly from
                    // DynamicProps. So in THIS proxy for getVals/setVals, we need to be sure not to fire PropSetListeners again.
                    for (Map.Entry<String, PropChange<?>> propChangeEntry : changedProps.entrySet()) {
                        // The eventual call to listener.reload is already wrapped in a try-catch, so don't wrap this
                        // in a superfluous try-catch. If an exception escapes from here, it is a library bug.
                        notifySingleListeners(propChangeEntry.getKey(), propChangeEntry.getValue(), affectedPropSetListeners);
                    }

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                setListener.remove();
                // See DynamicPropsSets.to(PropSetListener), which allows the possibility of registering a
                // PropSetListener to singular prop changes.
                DynamicProps.listener.remove();
                if (lockAcquired) {
                    lock.unlock();
                }
                if (cloneLock != null) {
                    cloneLock.unlock();
                }
            }

            return ret;
        }

        private void registerListener(PropSet<?> propSet, PropSetListener<?> listener) {
            for (String propKey : propSet.propKeys()) {
                Set<PropSetListener<?>> listenerSet = propsToSetListeners.get(propKey);
                if (listenerSet == null) {
                    propsToSetListeners.putIfAbsent(propKey, Collections.newSetFromMap(new ConcurrentHashMap<PropSetListener<?>, Boolean>()));
                    listenerSet = propsToSetListeners.get(propKey);
                }
                listenerSet.add(listener);
                // also add to singular prop change listeners
                DynamicPropsSets.super.registerListener(propKey, listener);
            }
        }

        /**
         * @param changedPropKey changed property key
         * @param propChange corresponding value change
         * @param affectedPropSetListeners these have already been notified, so don't do it again for singular props.
         */
        private void notifySingleListeners(String changedPropKey, PropChange<?> propChange, Set<PropSetListener<?>> affectedPropSetListeners) {
            Set<PropListener<?>> singlePropListeners = propsToSingleListeners.get(changedPropKey);
            if (singlePropListeners != null) {
                for (PropListener<?> singlePropListener : singlePropListeners) {
                    // Presumably PropSetListeners have already been taken care of, so just don't fire those. If this check
                    // isn't strong enough, consider something with set contains and add.
                    if (!(singlePropListener instanceof PropSetListener<?>)) {
                        notifyListener(singlePropListener, propChange);
                    } else {
                        assert affectedPropSetListeners.contains(singlePropListener);
                    }
                }
            }
        }

        /**
         * purely exists to limit scope of {@literal @}SuppressWarnings("unchecked")
         */
        @SuppressWarnings("unchecked")
        private void notifyListener(PropListener<?> listener, PropChange<?> propChange) {
            DynamicPropsSets.this.notifyListener((PropListener<Object>) listener, (PropChange<Object>) propChange);
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

        private Lock readLock(PropSet<?> propSet) {
            return getLock(propSet).readLock();
        }

        private Lock writeLock(PropSet<?> propSet) {
            return getLock(propSet).writeLock();
        }

    });

    public DynamicPropsSets() {
        // Without the cast this will actually go to the wrong constructor at runtime, even though following super
        // in IntelliJ goes to the correct one.
        super((PropsSets) new PropsSetsImpl());
    }

    public DynamicPropsSets(PropSource source) {
        super((PropsSets) new PropsSetsImpl(source));
    }

    /**
     * @param propSetListener who should register as a set listener on the properties of the corresponding propset
     *                        ({@link PropSetListener#propSet()} {@link PropSet#propKeys()})
     * @return this for chaining
     */
    public PropsSets to(final PropSetListener<?> propSetListener) {
        setListener.set(propSetListener);
        DynamicProps.listener.set(propSetListener); // Also allow a propset to register listeners on singular props
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
