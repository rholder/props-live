package com.github.dirkraft.propslive.dynamic.listen;

import com.github.dirkraft.propslive.dynamic.DynamicProps;
import com.github.dirkraft.propslive.set.PropSet;

/**
 * Pass an implementation to {@link DynamicProps#to(PropListener)} to register it against the following 'get'.
 * See {@link DynamicProps} for an example.
 * <p/>
 * For listening on {@link PropSet}s, use {@link PropSetListener}.
 *
 * @param <VALUE> type of property being listened on
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropListener<VALUE> {

    /**
     * Listener that should be triggered when a subscribed property is set.
     *
     * @param propChange in configuration that were just set
     */
    void reload(PropChange<VALUE> propChange);
}
