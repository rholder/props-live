package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.propsrc.PropSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration in the style of mapped properties. A great default implementation is available which is based on
 * {@link PropsImpl#PropsImpl() system properties}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface Props extends PropSource {

    /**
     * Collection of java types that are supported by the various getters/setters of this interface:
     * <ul>
     *     <li>Boolean</li>
     *     <li>Byte</li>
     *     <li>Character</li>
     *     <li>Double</li>
     *     <li>Enum</li>
     *     <li>Float</li>
     *     <li>Integer</li>
     *     <li>Long</li>
     *     <li>Short</li>
     *     <li>String</li>
     * </ul>
     */
    public static final Set<Class<?>> COVERED_CLASSES = Collections.<Class<?>>unmodifiableSet(new HashSet<>(Arrays.asList(
            Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            Character.class, String.class, Enum.class
    )));

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
