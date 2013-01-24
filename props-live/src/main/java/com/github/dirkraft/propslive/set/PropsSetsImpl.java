package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.DynamicProps;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.propsrc.view.ReadOnlyRestrictedPropSource;
import com.github.dirkraft.propslive.propsrc.view.RestrictedPropSource;

/**
 * Impl of {@link PropsSets}, a component of {@link DynamicProps} for atomically reading or writing sets of related
 * properties.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsSetsImpl extends PropsImpl implements PropsSets {

    /**
     * Backed by that of {@link PropsImpl#PropsImpl()}
     */
    public PropsSetsImpl() {
        super();
    }

    /**
     * Backed by arbitrary PropertySource
     *
     * @param source of props
     */
    public PropsSetsImpl(PropSource source) {
        super(source);
    }

    @Override
    public <VALUES> VALUES getVals(PropSet<VALUES> propSet) {
        return propSet.getVals(new PropsImpl(new ReadOnlyRestrictedPropSource(this, propSet.propKeys())));
    }

    @Override
    public void setVals(PropSet<?> propSet) {
        propSet.setVals(new PropsImpl(new RestrictedPropSource(this, propSet.propKeys())));
    }
}
