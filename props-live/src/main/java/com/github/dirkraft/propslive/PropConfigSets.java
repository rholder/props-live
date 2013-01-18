package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropConfigSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicConfig;

/**
 * Extension of PropConfig interface which adds logical support for sets of related properties. Also provides an
 * important layer of abstraction for proxies within further subclasses like {@link DynamicConfig}.
 *
 * @author jason
 */
public interface PropConfigSets extends PropConfig {
    /**
     * @param cfgSetReader that will construct a values set ({@link VALUES} impl) based on this PropConfig
     * @param <VALUES> wrapper class returned by the impl of {@link DefaultingPropConfigSetReader#getValuesSet(PropConfig)}
     * @return set of related property values encapsulated in the
     * @see DefaultingPropConfigSetReader#getValuesSet(PropConfig)
     */
    <VALUES extends PropConfigSetValues> VALUES get(DefaultingPropConfigSetReader<VALUES> cfgSetReader);

}
