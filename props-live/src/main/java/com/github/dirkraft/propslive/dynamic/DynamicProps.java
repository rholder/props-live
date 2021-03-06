package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.dynamic.listen.PropListener;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.set.PropsSets;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
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
public class DynamicProps<IMPL extends Props> implements Props {

    private static Logger logger = LoggerFactory.getLogger(DynamicProps.class);

    /**
     * Set by {@link #to(PropListener)} and read by {@link #proxy}
     */
    protected static final ThreadLocal<PropListener<?>> listener = new ThreadLocal<PropListener<?>>();

    /** Keys are String prop keys */
    private final ConcurrentHashMap<String, ReadWriteLock> propLocks = new ConcurrentHashMap<String, ReadWriteLock>();
    /**
     * Keys are String prop keys.
     */
    protected final ConcurrentHashMap<String, Set<PropListener<?>>> propsToSingleListeners = new ConcurrentHashMap<String, Set<PropListener<?>>>();

    /**
     * As a field, instead of having DynamicProps extend PropsSetsImpl, so that I can make sure that no methods are
     * correctly overridden. If DynamicProps extend PropsSetsImpl, it's possible to miss overriding a method to
     * delegate to the proxy.
     */
    protected final IMPL impl;

    /**
     * A special lock that can lock down all write interactions with this DynamicProps, particularly for thread-safe
     * cloning. Unfortunately we don't actually know the complete set of properties in the PropertySource, so we need
     * an "everything" lock, rather than actually obtaining every individual write lock.
     */
    protected final CloneLock cloneLock = new CloneLock();

    /**
     * All {@link Props} accesses go through here. All accesses will register the listener in {@link #listener} to the
     * interested property (String).
     */
    private final Props proxy = (Props) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Props.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock cloneLock = null;

            Lock lock = null;
            boolean propLockAcquired = false;

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
                    propLockAcquired = true;
                    ret = method.invoke(impl, args);

                } else if (set) {
                    cloneLock = DynamicProps.this.cloneLock.readLock(); // see javadoc and writeBlockLock
                    cloneLock.lock();

                    lock = writeLock(propKey);
                    propLockAcquired = lock.tryLock();

                    if (!propLockAcquired) {
                        throw new PropLockingException("Failed to acquire write lock for prop " + propKey + " as it " +
                                "was already locked.");
                    }

                    Method getter = PropsSets.NON_DEFAULTING_METHODS_BY_NAME.get(method.getName().replaceFirst("^set", "get"));
                    Object previous = getter.invoke(impl, propKey);
                    Object newVal = args[1];
                    if (!ObjectUtils.equals(previous, newVal)) {
                        notifyListeners(propKey, new PropChange<Object>(previous, newVal));
                    }
                    ret = method.invoke(impl, args);

                } else {
                    // carry on as usual
                    ret = method.invoke(impl, args);
                }

            } finally {
                listener.remove(); // reset affected listener
                if (propLockAcquired) {
                    lock.unlock(); // reset any owned lock
                }
                if (cloneLock != null) {
                    cloneLock.unlock();
                }
            }

            return ret;
        }

        private Lock readLock(String propKey) {
            return getLock(propKey).readLock();
        }

        private Lock writeLock(String propKey) {
            return getLock(propKey).writeLock();
        }
    });

    /**
     * Backed by that of {@link PropsImpl#PropsImpl()}
     */
    @SuppressWarnings("unchecked")
    public DynamicProps() {
        // Cast is necessary because self generic typing is not supported by any java compiler that I know of. The
        // 'correct' way would be to break out an additional AbstractDynamicProps<IMPL extends Props>. But that has
        // other implications, and a 6-character cast seems the better choice.
        this((IMPL) new PropsImpl());
    }

    /**
     * Backed by arbitrary {@link PropSource}
     *
     * @param source of props
     */
    @SuppressWarnings("unchecked")
    public DynamicProps(PropSource source) {
        this((IMPL) new PropsImpl(source));
    }

    public DynamicProps(IMPL impl) {
        this.impl = impl;
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

    protected ReadWriteLock getLock(String propKey) {
        ReadWriteLock lock = propLocks.get(propKey);
        if (lock == null) {
            propLocks.putIfAbsent(propKey, new ReentrantReadWriteLock());
            lock = propLocks.get(propKey);
        }
        return lock;
    }

    protected void registerListener(String propKey, PropListener<?> listener) {
        Set<PropListener<?>> listenerSet = propsToSingleListeners.get(propKey);
        if (listenerSet == null) {
            propsToSingleListeners.putIfAbsent(propKey, Collections.newSetFromMap(new ConcurrentHashMap<PropListener<?>, Boolean>()));
            listenerSet = propsToSingleListeners.get(propKey);
        }
        listenerSet.add(listener);
    }

    @SuppressWarnings("unchecked")
    protected <T> void notifyListeners(String propKey, PropChange<T> propChange) {
        Set<PropListener<?>> propListeners = propsToSingleListeners.get(propKey);
        if (propListeners != null) {
            for (PropListener<?> propListener : propListeners) {
                notifyListener((PropListener<T>) propListener, propChange);
            }
        }
    }

    protected <T> void notifyListener(PropListener<T> propListener, PropChange<T> propChange) {
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

    /**
     * @return an unmodifiable view of the underlying prop source's {@link PropSource#asMap()}.<ul>
     *     <li>default constructor: {@link PropsImpl#asMap()}</li>
     *     <li>arbitrary PropSource: that of the PropSource's asMap</li>
     *     <li>arbitrary Props: that of the Props' asMap </li>
     * </ul>
     * This should produce an accurate snapshot of all properties at the time of invocation, which requires the use of
     * a lock that must block all writes for the duration of the clone operation.
     */
    @Override
    public Map<String, String> asMap() {
        Lock cloneLock = this.cloneLock.writeLock();
        try {
            cloneLock.lock();
            return Collections.unmodifiableMap(impl.asMap());
        } finally {
            cloneLock.unlock();
        }
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

/**
 * Alias to {@link ReentrantReadWriteLock} to make code more readable, since we're taking advantage of the read/write
 * locks for not-exactly read/write locking. See {@link DynamicProps#cloneLock}.
 */
class CloneLock extends ReentrantReadWriteLock {
    /**
     * @return A lock that will lock immediately when no blocking lock has been granted ({@link #blockingLock()})
     */
    public ReadLock proceedingLock() {
        return super.readLock();
    }

    /**
     * @return A lock that will lock as soon as all {@link #proceedingLock()}s have been unlocked, and future
     *         proceedingLocks can be blocked for the duration of the blockingLock.
     */
    public WriteLock blockingLock() {
        return super.writeLock();
    }
}