package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.dynamic.DynamicProps;

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
public interface PropsSets extends Props {

    /**
     * Get a set of related properties.
     *
     * @param propSet that will produce some arbitrary type-safe pojo to contain whatever set of typed property vals
     * @param <VALUES> pojo class of properties
     * @return an instance of the pojo class as generated by the given {@link PropSet#getVals(Props)}, where the 'Props'
     *         argument will back lookups against this PropsSets instance, e.g. effectively propSet.getVals(this)
     * @throws IllegalPropertyAccessException if the propSet attempts to write to the 'props' in
     *         {@link PropSet#getVals(Props)} or accesses any properties not declared by its {@link PropSet#propKeys()}
     */
    <VALUES> VALUES getVals(PropSet<VALUES> propSet) throws IllegalPropertyAccessException;

    /**
     * Update a set of related properties.
     *
     * @param propSet that will apply whatever property changes through its {@link PropSet#setVals(Props)}, , e.g.
     *                effectively propSet.setVals(this)
     * @throws IllegalPropertyAccessException if the propSet attempts to access any properties not declared by its
     *         {@link PropSet#propKeys()}
     */
    void setVals(PropSet<?> propSet);

    public static Map<String, Method> NON_DEFAULTING_METHODS_BY_NAME = new HashMap<String, Method>(){{
        for (Method method : PropsSets.class.getMethods()) {
            if (method.getParameterTypes().length == 1) {
                put(method.getName(), method);
            }
        }
    }};
}
