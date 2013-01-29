package com.github.dirkraft.propslive.propsrc;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PropSource} backed by an arbitrary Map
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSourceMap implements PropSource {

    private final String description;
    private final Map<String, String> props;

    public PropSourceMap() {
        this("map property source");
    }

    public PropSourceMap(String description) {
        this(description, new HashMap<String, String>());
    }

    public PropSourceMap(Map<String, String> props) {
        this("map property source", props);
    }

    public PropSourceMap(String description, Map<String, String> props) {
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

    /**
     * @return the underlying map instance which was passed to the constructor, or the default (just a {@link HashMap})
     */
    @Override
    public Map<String, String> asMap() {
        return props;
    }
}
