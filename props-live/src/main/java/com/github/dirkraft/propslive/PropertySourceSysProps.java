package com.github.dirkraft.propslive;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropertySourceSysProps implements PropertySource {

    @Override
    public String description() {
        return "System properties";
    }

    @Override
    public String getProp(String key) {
        return System.getProperty(key);

    }

    @Override
    public PropertySource setProp(String key, String value) {
        System.setProperty(key, value);
        return this;
    }
}
