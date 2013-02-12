package com.github.dirkraft.propslive.core;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.DynamicPropsSets;
import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.set.PropSet;
import com.github.dirkraft.propslive.set.PropsSets;
import com.github.dirkraft.propslive.set.ease.PropSetAsPropSlice;
import com.github.dirkraft.propslive.set.ease.PropsSlice;

import java.util.Collection;
import java.util.Set;

/**
 * This is the uber class pretty much (it used to be called UberPropSet).<ul>
 *     <li>It is a {@link PropSet} and so can be passed to {@link PropsSets} or {@link DynamicPropsSets} as an atomic
 *         getter or setter for multiple properties.</li>
 *     <li>When passed to {@link PropsSets#getVals(PropSet)} it returns a {@link PropsSlice} which extends {@link Props}
 *         and affords all the type parsing conveniences of {@link PropsImpl}. This gives you the <strong>atomic read of multiple
 *         props</strong>.</li>
 *     <li>You can change its setter values as you would to {@link Props}, and then pass it to
 *         {@link DynamicPropsSets#setVals(PropSet)} for an <strong>atomic update of multiple props</strong>.</li>
 *     <li>It is a {@link PropSetListener} and so can hold the <strong>logic to perform when a {@link PropChange} event occurs</strong>.</li>
 * </ul>
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class LivePropSet extends PropSetAsPropSlice implements PropSetListener<PropsSlice> {

    public LivePropSet(String... propKeys) {
        super(propKeys);
    }

    public LivePropSet(Collection<String> propKeys) {
        super(propKeys);
    }

    public LivePropSet(Set<String> propKeys) {
        super(propKeys);
    }

    public LivePropSet(PropSource source, String... propKeys) {
        super(source, propKeys);
    }

    public LivePropSet(PropSource source, Collection<String> propKeys) {
        super(source, propKeys);
    }

    public LivePropSet(PropSource source, Set<String> propKeys) {
        super(source, propKeys);
    }

    @Override
    public LivePropSet propSet() {
        return this;
    }

    /**
     * Override me to perform something on reload
     */
    @Override
    public void reload(PropChange<PropsSlice> propChange) {
        // do nothing
    }
}
