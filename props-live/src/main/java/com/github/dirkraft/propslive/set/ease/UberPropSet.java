package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.DynamicPropsSets;
import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.set.PropSet;
import com.github.dirkraft.propslive.set.PropsSets;

import java.util.Collection;
import java.util.Set;

/**
 * This is the uber class pretty much.<ul>
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
 * <hr/>
 *
 * A fully-fleshed out example:
 * <pre>
 *
 * </pre>
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public abstract class UberPropSet extends PropSetAsPropSlice implements PropSetListener<PropsSlice> {

    public UberPropSet(String... propKeys) {
        super(propKeys);
    }

    public UberPropSet(Collection<String> propKeys) {
        super(propKeys);
    }

    public UberPropSet(Set<String> propKeys) {
        super(propKeys);
    }

    public UberPropSet(PropSource source, String... propKeys) {
        super(source, propKeys);
    }

    public UberPropSet(PropSource source, Collection<String> propKeys) {
        super(source, propKeys);
    }

    public UberPropSet(PropSource source, Set<String> propKeys) {
        super(source, propKeys);
    }

    @Override
    public UberPropSet propSet() {
        return this;
    }
}
