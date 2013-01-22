package com.github.dirkraft.propslive.propsrc;

import java.util.HashMap;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropertySourceMap extends HashMap<String, String> implements PropertySource {

    private final String description;

    public PropertySourceMap() {
        this.description = "map property source";
    }

    public PropertySourceMap(String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String getProp(String key) {
        return this.get(key);
    }

    @Override
    public void setProp(String key, String value) {
        put(key, value);
    }
}
