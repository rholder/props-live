package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicProps;
import com.github.dirkraft.propslive.propsrc.PropertySource;

/**
 * Adds methods declared by {@link PropsSets}. As mentioned there, the interface itself is useful for proxying
 * within other classes like {@link DynamicProps}.
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
    public <VALUES extends PropSetKeys> VALUES get(DefaultingPropSetReader<VALUES> propSetReader) {
        return propSetReader.getValuesSet(this);
    }
}
