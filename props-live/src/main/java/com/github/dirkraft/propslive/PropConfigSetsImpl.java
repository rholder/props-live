package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropConfigSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicConfig;

/**
 * Adds methods declared by {@link PropConfigSets}. As mentioned there, the interface itself is useful for proxying
 * within other classes like {@link DynamicConfig}.
 *
 * @author jason
 */
public class PropConfigSetsImpl extends PropConfigImpl implements PropConfigSets {

    public PropConfigSetsImpl() {
        super();
    }

    protected PropConfigSetsImpl(PropertySource source) {
        super(source);
    }

    @Override
    public <VALUES extends PropConfigSetValues> VALUES get(DefaultingPropConfigSetReader<VALUES> cfgSetReader) {
        return cfgSetReader.getValuesSet(this);
    }
}
