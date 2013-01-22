package com.github.dirkraft.propslive;

/**
 * Configuration in the style of mapped properties. A great default implementation is available which is based on
 * {@link PropsImpl#PropsImpl() system properties}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface Props {

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

    /* ***** setters must match getters ***** */

    void setBool(String key, Boolean value);
    void setByte(String key, Byte value);
    void setShort(String key, Short value);
    void setInt(String key, Integer value);
    void setLong(String key, Long value);
    void setFloat(String key, Float value);
    void setDouble(String key, Double value);
    void setCharacter(String key, Character value);
    void setString(String key, String value);
    <T extends Enum<T>> void setEnum(String key, T value);
}
