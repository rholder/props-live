package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.set.PropSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Convenience on a {@link PropSet} to return a {@link PropsImpl}-like object from {@link #getVals(Props)} to take
 * advantage of the type-parsing methods of {@link PropsImpl}. The returned {@link PropsSlice} is disconnected from
 * the Props fed to getVals and so may be safely read from and modified at any time. For the corollary
 * {@link #setVals(Props)} operation, values will be read from this PropSlice and put into the given Props arg. So this
 * class is dual purpose for either reading or writing or both.
 * <p/>
 *
 * This provides somewhat of an alternative to implementing a custom pojo holder class (that returned by
 * {@link #getVals(Props)}, and setter logic in {@link #setVals(Props)}). It is almost entirely a matter of preference,
 * as the both custom, typed holder pojos vs general-purpose PropSlices serve exactly the same purpose, which is to
 * provide a holder object for a set of related properties, i.e. a {@link PropSet}.
 * <p/>
 *
 * Great care should be taken to distinguish various PropSlices from any central/master Props objects, as this could
 * become quickly confusing.
 *
 * <hr/>
 * Sample usage:
 * <pre>
 *
 * </pre>
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSetAsPropSlice extends PropsSlice implements PropSet<PropsSlice> {

    private final Set<String> propKeys;

    public PropSetAsPropSlice(String... propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropSetAsPropSlice(String...)"), propKeys);
    }

    public PropSetAsPropSlice(Collection<String> propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropSetAsPropSlice(Collection<String>)"), propKeys);
    }

    public PropSetAsPropSlice(Set<String> propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropSetAsPropSlice(Set<String>)"), propKeys);
    }

    /**
     * @param source to clone vals from
     */
    public PropSetAsPropSlice(PropSource source, String... propKeys) {
        this(source, Arrays.asList(propKeys));
    }

    /**
     * @param source to clone vals from
     */
    public PropSetAsPropSlice(final PropSource source, final Collection<String> propKeys) {
        this(source, new HashSet<>(propKeys));
    }

    /**
     * @param source to clone vals from
     */
    public PropSetAsPropSlice(final PropSource source, final Set<String> propKeys) {
        super(source, propKeys);
        this.propKeys = Collections.unmodifiableSet(new HashSet<>(propKeys));
    }

    /**
     * @return set of prop keys represented by this slice. This collection is immutable.
     */
    @Override
    public Set<String> propKeys() {
        return propKeys;
    }

    /**
     * This will copy values from 'props' for all of the prop keys of this PropsSlice.
     * @return this
     */
    @Override
    public PropsSlice getVals(Props props) {
        // copy values from given props into this
        for (String propKey : propKeys) {
            this.setString(propKey, props.getString(propKey));
        }
        return this;
    }

    /**
     * This will copy values from this PropSlice into the given 'props'
     */
    @Override
    public void setVals(Props props) {
        // copy values from this into given props
        for (String propKey : propKeys) {
            props.setString(propKey, this.getString(propKey));
        }
    }
}
