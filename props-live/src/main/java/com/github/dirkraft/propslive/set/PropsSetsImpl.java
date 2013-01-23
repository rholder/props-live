package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.DynamicProps;
import com.github.dirkraft.propslive.propsrc.PropertySource;

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
    public PropsSetsImpl(PropertySource source) {
        super(source);
    }

    @Override
    public <VALUES> VALUES getVals(PropSet<VALUES> propSet) {
        return propSet.getVals(this);
    }

    @Override
    public <VALUES> void setVals(PropSet<VALUES> propSet) {
        propSet.setVals(this);
    }

}
