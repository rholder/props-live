package com.github.dirkraft.propslive;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Properties;

/**
 * Implementation of PropConfig which does some basic String manip (trim, blank checkes) before delegating all access
 * to a {@link PropertySource}.
 *
 * @author jason
 */
public class PropConfigImpl implements PropConfig {

    /**
     * Base abstraction of where to read/write properties. About as simple as {@link Properties} or a
     * {@link Map Map&lt;String, String&gt;}
     */
    protected interface PropertySource {
        /**
         * @return description to help identify this property source, e.g. system properties, config.properties file, ...
         */
        String description();

        /**
         * @param key name of the property
         * @return corresponding value or <code>null</code> if the property was not set (or set to <code>null</code>,
         *         you devil, you)
         */
        String getProp(String key);

        /**
         * @param key to put value against
         * @param value to store at the key
         */
        PropertySource setProp(String key, String value);
    }

    protected final PropertySource source;

    /**
     * Defaults the {@link #source} to system properties.
     */
    public PropConfigImpl() {
        source = new PropertySource() {
            @Override
            public String description() {
                return "System properties";
            }

            @Override
            public String getProp(String key) {
                return System.getProperty(key);

            }

            @Override
            public PropertySource setProp(String key, String value) {
                System.setProperty(key, value);
                return this;
            }
        };
    }

    /**
     * @param source accept an arbitrary PropertySource instead of using system properties
     */
    public PropConfigImpl(PropertySource source) {
        this.source = source;
    }

    @Override
    public final String description() {
        return source.description();
    }

    /* GETTERS */

    @Override
    public Boolean getBool(String key) {
        return getBool(key, null);
    }

    @Override
    public Boolean getBool(String key, Boolean def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Boolean.valueOf(strval);
    }

    @Override
    public Byte getByte(String key) {
        return getByte(key, null);
    }

    @Override
    public Byte getByte(String key, Byte def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Byte.valueOf(strval);
    }

    @Override
    public Short getShort(String key) {
        return getShort(key, null);
    }

    @Override
    public Short getShort(String key, Short def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Short.valueOf(strval);
    }

    @Override
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    @Override
    public Integer getInt(String key, Integer def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Integer.valueOf(strval);
    }

    @Override
    public Long getLong(String key) {
        return getLong(key, null);
    }

    @Override
    public Long getLong(String key, Long def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Long.valueOf(strval);
    }

    @Override
    public Float getFloat(String key) {
        return getFloat(key, null);
    }

    @Override
    public Float getFloat(String key, Float def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Float.valueOf(strval);
    }

    @Override
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    @Override
    public Double getDouble(String key, Double def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Double.valueOf(strval);
    }

    @Override
    public Character getChar(String key) {
        return getChar(key, null);
    }

    @Override
    public Character getChar(String key, Character def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : strval.charAt(0);
    }

    @Override
    public String getString(String key) {
        return getString(key, null);
    }

    @Override
    public String getString(String key, String def) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : strval;
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumCls) {
        return getEnum(key, null, enumCls);
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls) {
        String strval = source.getProp(key);
        return StringUtils.isBlank(strval) ? def : Enum.valueOf(enumCls, strval);
    }

    /* SETTERS */

    @Override
    public <T extends Enum<T>> void set(String key, T value) {
        source.setProp(key, value.name());
    }

    @Override
    public void set(String key, Boolean value) {
        source.setProp(key, Boolean.toString(value));
    }

    @Override
    public void set(String key, Byte value) {
        source.setProp(key, Byte.toString(value));
    }

    @Override
    public void set(String key, Short value) {
        source.setProp(key, Short.toString(value));
    }

    @Override
    public void set(String key, Integer value) {
        source.setProp(key, Integer.toString(value));
    }

    @Override
    public void set(String key, Long value) {
        source.setProp(key, Long.toString(value));
    }

    @Override
    public void set(String key, Float value) {
        source.setProp(key, Float.toString(value));
    }

    @Override
    public void set(String key, Double value) {
        source.setProp(key, Double.toString(value));
    }

    @Override
    public void set(String key, Character value) {
        source.setProp(key, Character.toString(value));
    }

    @Override
    public void set(String key, String value) {
        source.setProp(key, value);
    }

}