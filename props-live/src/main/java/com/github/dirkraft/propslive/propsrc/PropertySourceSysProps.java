package com.github.dirkraft.propslive.propsrc;

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
    public void setProp(String key, String value) {
        System.setProperty(key, value);
    }
}
