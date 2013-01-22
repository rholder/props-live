package com.github.dirkraft.propslive.dynamic;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public interface DynamicPropListener<VALUE> {

    /**
     * Listener that should be triggered when a subscribed property is set.
     *
     * @param values in configuration that were just set
     */
    public void reload(PropChange<VALUE> values);
}
