package com.github.dirkraft.propslive;

/**
 * Configuration in the style of mapped properties. A great default implementation is available which is based on
 * {@link PropConfigImpl#PropConfigImpl() system properties}.
 *
 * @author jason
 */
public interface PropConfig {

    /**
     * @return a description of the property source behind this PropConfig
     */
    String description();

    Boolean getBool(String key);
    Boolean getBool(String key, Boolean def);

    Byte getByte(String key);
    Byte getByte(String key, Byte def);

    Short getShort(String key);
    Short getShort(String key, Short def);

    Integer getInt(String key);
    Integer getInt(String key, Integer def);

    Long getLong(String key);
    Long getLong(String key, Long def);

    Float getFloat(String key);
    Float getFloat(String key, Float def);

    Double getDouble(String key);
    Double getDouble(String key, Double def);

    Character getChar(String key);
    Character getChar(String key, Character def);

    String getString(String key);
    String getString(String key, String def);

    <E extends Enum<E>> E getEnum(String key, Class<E> enumCls);
    <E extends Enum<E>> E getEnum(String key, E def, Class<E> enumCls);

    void set(String key, Boolean value);
    void set(String key, Byte value);
    void set(String key, Short value);
    void set(String key, Integer value);
    void set(String key, Long value);
    void set(String key, Float value);
    void set(String key, Double value);
    void set(String key, Character value);
    void set(String key, String value);
    <T extends Enum<T>> void set(String key, T value);
}