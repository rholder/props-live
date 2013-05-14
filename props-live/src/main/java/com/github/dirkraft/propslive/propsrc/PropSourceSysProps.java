package com.github.dirkraft.propslive.propsrc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSourceSysProps implements PropSource {

    @Override
    public String description() {
        return "System properties";
    }

    @Override
    public String getString(String key) {
        return System.getProperty(key);

    }

    @Override
    public void setString(String key, String value) {
        System.setProperty(key, value);
    }

    /**
     * @return a copy of system properties, with all members {@link Object#toString()}'d, since system {@link Properties}
     *         is actually a Map&lt;Object, Object&gt;
     */
    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }
}
