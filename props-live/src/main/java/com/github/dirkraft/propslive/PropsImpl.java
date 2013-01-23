package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.propsrc.PropertySource;
import com.github.dirkraft.propslive.propsrc.PropertySourceSysProps;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of PropConfig which does some basic String manip (trim, blank checkes) before delegating all access
 * to a {@link PropertySource}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsImpl implements Props {

    public static Props make() {
        return new PropsImpl();
    }

    protected final PropertySource source;

    /**
     * Defaults the {@link #source} to system properties.
     */
    public PropsImpl() {
        source = new PropertySourceSysProps();
    }

    /**
     * @param source accept an arbitrary PropertySource
     */
    public PropsImpl(PropertySource source) {
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
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Boolean.valueOf(strval);
    }

    @Override
    public Byte getByte(String key) {
        return getByte(key, null);
    }

    @Override
    public Byte getByte(String key, Byte def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Byte.valueOf(strval);
    }

    @Override
    public Short getShort(String key) {
        return getShort(key, null);
    }

    @Override
    public Short getShort(String key, Short def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Short.valueOf(strval);
    }

    @Override
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    @Override
    public Integer getInt(String key, Integer def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Integer.valueOf(strval);
    }

    @Override
    public Long getLong(String key) {
        return getLong(key, null);
    }

    @Override
    public Long getLong(String key, Long def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Long.valueOf(strval);
    }

    @Override
    public Float getFloat(String key) {
        return getFloat(key, null);
    }

    @Override
    public Float getFloat(String key, Float def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Float.valueOf(strval);
    }

    @Override
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    @Override
    public Double getDouble(String key, Double def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Double.valueOf(strval);
    }

    @Override
    public Character getChar(String key) {
        return getChar(key, null);
    }

    @Override
    public Character getChar(String key, Character def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : strval.charAt(0);
    }

    @Override
    public String getString(String key) {
        return getString(key, null);
    }

    @Override
    public String getString(String key, String def) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : strval;
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumCls) {
        return getEnum(key, null, enumCls);
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls) {
        String strval = source.getString(key);
        return StringUtils.isBlank(strval) ? def : Enum.valueOf(enumCls, strval);
    }

    /* SETTERS */

    @Override
    public <T extends Enum<T>> void setEnum(String key, T value) {
        source.setString(key, value.name());
    }

    @Override
    public void setBool(String key, Boolean value) {
        source.setString(key, Boolean.toString(value));
    }

    @Override
    public void setByte(String key, Byte value) {
        source.setString(key, Byte.toString(value));
    }

    @Override
    public void setShort(String key, Short value) {
        source.setString(key, Short.toString(value));
    }

    @Override
    public void setInt(String key, Integer value) {
        source.setString(key, Integer.toString(value));
    }

    @Override
    public void setLong(String key, Long value) {
        source.setString(key, Long.toString(value));
    }

    @Override
    public void setFloat(String key, Float value) {
        source.setString(key, Float.toString(value));
    }

    @Override
    public void setDouble(String key, Double value) {
        source.setString(key, Double.toString(value));
    }

    @Override
    public void setCharacter(String key, Character value) {
        source.setString(key, Character.toString(value));
    }

    @Override
    public void setString(String key, String value) {
        source.setString(key, value);
    }

}