package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.PropConfigSetValues;
import com.github.dirkraft.propslive.PropConfigSets;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides atomic configuration operations, as well as automatically subscribes listeners to configuration events
 * based on the properties they read.
 *
 * @author jason
 */
public class DynamicConfig {

    /**
     * Proxies accesses to props to synchronize over the entire configuration set (as long as everything accesses config
     * this way). Note that the underlying props source is provided externally.
     */
    private final PropConfigSets props;

    public DynamicConfig(final PropConfigSets propConfigSets) {
        props = (PropConfigSets) Proxy.newProxyInstance(
                DynamicConfig.class.getClassLoader(), new Class<?>[]{PropConfigSets.class},
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

    public <VALUES extends PropConfigSetValues> VALUES get(DefaultingPropConfigSetReader<VALUES> cfgSet, DynamicConfigListener dynCfgListener) {
        return props.get(cfgSet);
    }

    /* START delegate methods (generated), but with DynamicConfigListener callback added to all signatures */

    public Boolean getBool(String key, DynamicConfigListener dynCfgListener) {
        return props.getBool(key);
    }

    public Boolean getBool(String key, Boolean def, DynamicConfigListener dynCfgListener) {
        return props.getBool(key, def);
    }

    public Byte getByte(String key, DynamicConfigListener dynCfgListener) {
        return props.getByte(key);
    }

    public Byte getByte(String key, Byte def, DynamicConfigListener dynCfgListener) {
        return props.getByte(key, def);
    }

    public Short getShort(String key, DynamicConfigListener dynCfgListener) {
        return props.getShort(key);
    }

    public Short getShort(String key, Short def, DynamicConfigListener dynCfgListener) {
        return props.getShort(key, def);
    }

    public Integer getInt(String key, DynamicConfigListener dynCfgListener) {
        return props.getInt(key);
    }

    public Integer getInt(String key, Integer def, DynamicConfigListener dynCfgListener) {
        return props.getInt(key, def);
    }

    public Long getLong(String key, DynamicConfigListener dynCfgListener) {
        return props.getLong(key);
    }

    public Long getLong(String key, Long def, DynamicConfigListener dynCfgListener) {
        return props.getLong(key, def);
    }

    public Float getFloat(String key, DynamicConfigListener dynCfgListener) {
        return props.getFloat(key);
    }

    public Float getFloat(String key, Float def, DynamicConfigListener dynCfgListener) {
        return props.getFloat(key, def);
    }

    public Double getDouble(String key, DynamicConfigListener dynCfgListener) {
        return props.getDouble(key);
    }

    public Double getDouble(String key, Double def, DynamicConfigListener dynCfgListener) {
        return props.getDouble(key, def);
    }

    public Character getChar(String key, DynamicConfigListener dynCfgListener) {
        return props.getChar(key);
    }

    public Character getChar(String key, Character def, DynamicConfigListener dynCfgListener) {
        return props.getChar(key, def);
    }

    public String getString(String key, DynamicConfigListener dynCfgListener) {
        return props.getString(key);
    }

    public String getString(String key, String def, DynamicConfigListener dynCfgListener) {
        return props.getString(key, def);
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> enumCls, DynamicConfigListener dynCfgListener) {
        return props.getEnum(key, enumCls);
    }

    public <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls, DynamicConfigListener dynCfgListener) {
        return props.getEnum(key, def, enumCls);
    }

    public void set(String key, Boolean value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Byte value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Short value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Integer value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Long value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Float value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Double value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, Character value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public void set(String key, String value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

    public <T extends Enum<T>> void set(String key, T value, DynamicConfigListener dynCfgListener) {
        props.set(key, value);
    }

}
