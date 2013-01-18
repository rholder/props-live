package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicProps;

/**
 * Adds methods declared by {@link PropsSets}. As mentioned there, the interface itself is useful for proxying
 * within other classes like {@link DynamicProps}.
 *
 * @author jason
 */
public class PropsSetsImpl extends PropsImpl implements PropsSets {

    /**
     * Backed by {@link PropsImpl#PropsImpl()}
     */
    public PropsSetsImpl() {
        super();
    }

    protected PropsSetsImpl(PropertySource source) {
        super(source);
    }

    @Override
    public <VALUES extends PropsSet> VALUES get(DefaultingPropSetReader<VALUES> cfgSetReader) {
        return cfgSetReader.getValuesSet(this);
    }
}
