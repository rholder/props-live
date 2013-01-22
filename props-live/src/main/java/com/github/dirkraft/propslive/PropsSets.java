package com.github.dirkraft.propslive;

import com.github.dirkraft.propslive.dynamic.DefaultingPropSetReader;
import com.github.dirkraft.propslive.dynamic.DynamicProps;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension of PropConfig interface which adds logical support for sets of related properties. Also provides an
 * important layer of abstraction for proxies within further subclasses like {@link DynamicProps}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropsSets extends Props {
    /**
     * @param propSetReader that will construct a values set based on this PropConfig
     * @param <VALUES> wrapper class returned by the impl of {@link DefaultingPropSetReader#getValuesSet(Props)}
     * @return set of related property values encapsulated in the
     * @see DefaultingPropSetReader#getValuesSet(Props)
     */
    <VALUES extends PropSetKeys> VALUES get(DefaultingPropSetReader<VALUES> propSetReader);

    public static Map<String, Method> NON_DEFAULTING_METHODS_BY_NAME = new HashMap<String, Method>(){{
        for (Method method : PropsSets.class.getMethods()) {
            if (method.getParameterTypes().length == 1) {
                put(method.getName(), method);
            }
        }
    }};
}
