package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.set.PropSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Convenience abstract impl of {@link PropSet} with all methods defaulted to no-op. Useful when you may only care about
 * one of reading or writing a PropSet.
 * <hr/>
 * Example: I only care about reads, so I don't want an empty {@link PropSet#setVals(Props)} impl cluttering my code.
 * <pre>
 * PropSet&lt;MyValueHolder&gt; myPropSet = new AbstractPropSet&lt;MyValueHolder&gt;("my.prop.url", "my.prop.user", "my.prop.pass") {
 *     {@literal@}Override
 *     public MyValueHolder getVals(Props props) {
 *         return new MyValueHolder(
 *             props.get("my.prop.url"),
 *             props.get("my.prop.user"),
 *             props.get("my.prop.pass")
 *         );
 *     }
 * }
 * </pre>
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class AbstractPropSet<VALUES> implements PropSet<VALUES> {

    private final Set<String> propKeys;

    public AbstractPropSet(String... propKeys) {
        this(Arrays.asList(propKeys));
    }

    public AbstractPropSet(Collection<String> propKeys) {
        this(new LinkedHashSet<>(propKeys));
    }

    public AbstractPropSet(Set<String> propKeys) {
        this.propKeys = Collections.unmodifiableSet(propKeys);
    }

    @Override
    public Set<String> propKeys() {
        return propKeys;
    }

    @Override
    public VALUES getVals(Props props) {
        return null; // stub impl
    }

    @Override
    public void setVals(Props props) {
        // stub impl
    }

}
