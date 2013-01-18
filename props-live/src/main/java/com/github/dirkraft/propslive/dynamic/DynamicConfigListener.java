package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.PropConfigSetValues;

/**
 * @author jason
 */
public interface DynamicConfigListener<VALUES extends PropConfigSetValues> {

    /**
     * Listener that should be triggered when a subscribed property is set.
     *
     * @param values in configuration that were just set
     */
    public void reload(VALUES values);
}
