package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.dynamic.PropChange;

/**
 * Pass an implementation to {@link DynamicProps#to(PropListener)} to register it against the following 'get'.
 * See {@link DynamicProps} for an example.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropListener<VALUE> {

    /**
     * Listener that should be triggered when a subscribed property is set.
     *
     * @param values in configuration that were just set
     */
    public void reload(PropChange<VALUE> values);
}
