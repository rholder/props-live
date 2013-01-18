package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.PropsSet;
import com.github.dirkraft.propslive.PropsSets;
import com.github.dirkraft.propslive.PropsSetsImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides atomic configuration operations, as well as automatically subscribes listeners to configuration events
 * based on the properties they read.
 *
 * @author jason
 */
public class DynamicProps {

    /**
     * Proxies accesses to props to synchronize over the entire configuration set (as long as everything accesses config
     * this way). Note that the underlying props source is provided externally.
     */
    private final PropsSets props;

    /**
     * Backed by {@link PropsSetsImpl#PropsSetsImpl()}
     */
    public DynamicProps() {
        this(new PropsSetsImpl());
    }

    public DynamicProps(final PropsSets propConfigSets) {
        props = (PropsSets) Proxy.newProxyInstance(
                DynamicProps.class.getClassLoader(), new Class<?>[]{PropsSets.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        synchronized (propConfigSets) {
                            return method.invoke(propConfigSets, args);
                        }
                    }
                }
        );
    }

    public <VALUES extends PropsSet> VALUES get(DefaultingPropSetReader<VALUES> cfgSet, DynamicPropsListener dynCfgListener) {
        return props.get(cfgSet);
    }

    /* START delegate methods (generated), but with DynamicConfigListener callback added to all signatures */

    public Boolean getBool(String key, DynamicPropsListener dynCfgListener) {
        return props.getBool(key);
    }

    public Boolean getBool(String key, Boolean def, DynamicPropsListener dynCfgListener) {
        return props.getBool(key, def);
    }

    public Byte getByte(String key, DynamicPropsListener dynCfgListener) {
        return props.getByte(key);
    }

    public Byte getByte(String key, Byte def, DynamicPropsListener dynCfgListener) {
        return props.getByte(key, def);
    }

    public Short getShort(String key, DynamicPropsListener dynCfgListener) {
        return props.getShort(key);
    }

    public Short getShort(String key, Short def, DynamicPropsListener dynCfgListener) {
        return props.getShort(key, def);
    }

    public Integer getInt(String key, DynamicPropsListener dynCfgListener) {
        return props.getInt(key);
    }

    public Integer getInt(String key, Integer def, DynamicPropsListener dynCfgListener) {
        return props.getInt(key, def);
    }

    public Long getLong(String key, DynamicPropsListener dynCfgListener) {
        return props.getLong(key);
    }

    public Long getLong(String key, Long def, DynamicPropsListener dynCfgListener) {
        return props.getLong(key, def);
    }

    public Float getFloat(String key, DynamicPropsListener dynCfgListener) {
        return props.getFloat(key);
    }

    public Float getFloat(String key, Float def, DynamicPropsListener dynCfgListener) {
        return props.getFloat(key, def);
    }

    public Double getDouble(String key, DynamicPropsListener dynCfgListener) {
        return props.getDouble(key);
    }

    public Double getDouble(String key, Double def, DynamicPropsListener dynCfgListener) {
        return props.getDouble(key, def);
    }

    public Character getChar(String key, DynamicPropsListener dynCfgListener) {
        return props.getChar(key);
    }

    public Character getChar(String key, Character def, DynamicPropsListener dynCfgListener) {
        return props.getChar(key, def);
    }

    public String getString(String key, DynamicPropsListener dynCfgListener) {
        return props.getString(key);
    }

    public String getString(String key, String def, DynamicPropsListener dynCfgListener) {
        return props.getString(key, def);
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> enumCls, DynamicPropsListener dynCfgListener) {
        return props.getEnum(key, enumCls);
    }

    public <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls, DynamicPropsListener dynCfgListener) {
        return props.getEnum(key, def, enumCls);
    }

    public void set(String key, Boolean value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Byte value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Short value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Integer value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Long value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Float value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Double value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Character value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, String value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

    public <T extends Enum<T>> void set(String key, T value, DynamicPropsListener dynCfgListener) {
        props.set(key, value);
    }

}
