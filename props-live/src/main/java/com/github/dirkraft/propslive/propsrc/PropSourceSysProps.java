package com.github.dirkraft.propslive.propsrc;

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
}
