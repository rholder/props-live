package com.github.dirkraft.propslive.propsrc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropertySourceMap implements PropertySource {

    private final String description;
    private final Map<String, String> props;

    public PropertySourceMap() {
        this("map property source");
    }

    public PropertySourceMap(String description) {
        this(description, new HashMap<String, String>());
    }

    public PropertySourceMap(Map<String, String> props) {
        this("map property source", props);
    }

    public PropertySourceMap(String description, Map<String, String> props) {
        this.description = description;
        this.props = props;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String getString(String key) {
        return props.get(key);
    }

    @Override
    public void setString(String key, String value) {
        props.put(key, value);
    }


}
