package com.github.dirkraft.propslive.dynamic.listen;

import com.github.dirkraft.propslive.dynamic.PropChange;
import com.github.dirkraft.propslive.dynamic.PropSet;

/**
 * Corollary to {@link PropListener} which provides a corresponding change listener interface for {@link PropSet}s
 *
 * @param <VALUES> corresponding pojo of type-safe values to the PropSet impl returned by {@link #propSet()}
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropSetListener<VALUES> extends PropListener<VALUES> {

    /**
     * @return corresponding {@link PropSet} to this listener
     */
    PropSet<VALUES> propSet();

    /**
     * Listener that should be triggered when a subscribed property is set. A PropSetListener is subscribed to changes
     * on every property of its corresponding PropSet (defined by {@link PropSet#propKeys()})
     *
     * @param values in configuration that were just set
     */
    @Override
    void reload(PropChange<VALUES> values);

}
