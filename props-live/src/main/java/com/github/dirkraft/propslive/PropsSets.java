package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicProps;

/**
 * Extension of PropConfig interface which adds logical support for sets of related properties. Also provides an
 * important layer of abstraction for proxies within further subclasses like {@link DynamicProps}.
 *
 * @author jason
 */
public interface PropsSets extends Props {
    /**
     * @param cfgSetReader that will construct a values set based on this PropConfig
     * @param <VALUES> wrapper class returned by the impl of {@link DefaultingPropSetReader#getValuesSet(Props)}
     * @return set of related property values encapsulated in the
     * @see DefaultingPropSetReader#getValuesSet(Props)
     */
    <VALUES extends PropsSet> VALUES get(DefaultingPropSetReader<VALUES> cfgSetReader);

}
