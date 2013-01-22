package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension of PropConfig interface which adds support for atomically reading and writing sets of related properties
 * through {@link DynamicProps}.
 * <p/>
 * The processing for reading/writing one particular set of properties should extend {@link PropSet}
 *
 * @author Jason Dunkelberger (dirkraft)
 */
interface PropsSets extends Props {

    /**
     * Get a set of related properties which when
     *
     * @param propSet
     * @param <VALUES>
     * @return
     */
    <VALUES> VALUES getVals(PropSet<VALUES> propSet);

    /**
     *
     *
     * @param propSet
     * @param <VALUES>
     */
    <VALUES> void setVals(PropSet<VALUES> propSet);

    public static Map<String, Method> NON_DEFAULTING_METHODS_BY_NAME = new HashMap<String, Method>(){{
        for (Method method : PropsSets.class.getMethods()) {
            if (method.getParameterTypes().length == 1) {
                put(method.getName(), method);
            }
        }
    }};
}
