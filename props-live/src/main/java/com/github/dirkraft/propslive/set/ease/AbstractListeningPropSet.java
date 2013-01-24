package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.dynamic.listen.PropSetListener;
import com.github.dirkraft.propslive.set.PropSet;

import java.util.Collection;
import java.util.Set;

/**
 * Convenience pattern to define both a {@link PropSet} and {@link PropSetListener} functionality in the same
 * object, rather than having functionality spread across two separate types. Sometimes it may be beneficial to
 * co-locate functionality around the same VALUES pojo object.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public abstract class AbstractListeningPropSet<VALUES> extends AbstractPropSet<VALUES> implements PropSetListener<VALUES> {

    protected AbstractListeningPropSet(String... propKeys) {
        super(propKeys);
    }

    protected AbstractListeningPropSet(Collection<String> propKeys) {
        super(propKeys);
    }

    protected AbstractListeningPropSet(Set<String> propKeys) {
        super(propKeys);
    }

    @Override
    public PropSet<VALUES> propSet() {
        return this;
    }
}
