package com.github.dirkraft.propslive;

import java.util.Map;
import java.util.Properties;

/**
 * Base abstraction of where to read/write properties. About as simple as {@link Properties} or a
 * {@link Map Map&lt;String, String&gt;}
 */
public interface PropertySource {
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
