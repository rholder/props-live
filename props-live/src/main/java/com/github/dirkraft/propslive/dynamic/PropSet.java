package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;

import java.util.List;

/**
 * Interface for type-safe logic that will read and write sets of related properties. Such 'sets' gain atomic semantics
 * through { com.github.dirkraft.propslive.dynamic.DynamicProps}.
 *
 * @param <VALUES> the pojo that encapsulates the corresponding value set in a type-safe way
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface PropSet<VALUES> {

    /**
     * @return the set of prop keys that comprise the interest of this PropSet
     */
    List<String> propKeys();

    /**
     * Accesses the given props atomically.
     *
     * @param props to read values from. Only keys specified by {@link #propKeys()} will be available in this view.
     * @return pojo that encapsulates the corresponding value set in a type-safe way
     */
    VALUES getVals(Props props);

    /**
     * @param props that will receive modifications. Only keys specified by {@link #propKeys()} may be modified through
     *              this view.
     */
    void setVals(Props props);

}
