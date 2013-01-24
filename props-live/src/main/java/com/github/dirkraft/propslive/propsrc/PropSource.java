package com.github.dirkraft.propslive.propsrc;

import java.util.Map;
import java.util.Properties;

/**
 * Base abstraction of where to read/write properties. About as simple as {@link Properties} or a
 * {@link Map Map&lt;String, String&gt;}. A strong assumption made is that all property values can be represented as
 * Strings.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropSource {

    /**
     * @return description to help identify this property source, e.g. system properties, config.properties file, ...
     */
    String description();

    /**
     * @param key name of the property
     * @return corresponding value or <code>null</code> if the property was not set (or set to <code>null</code>,
     *         you devil, you)
     */
    String getString(String key);

    /**
     * @param key to put value against
     * @param value to store at the key
     */
    void setString(String key, String value);
}
