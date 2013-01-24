package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.set.PropSet;

import java.util.Set;

/**
 * That of {@link AbstractListeningPropSet} and which accepts an external delegating {@link PropSet} impl.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public abstract class DelegatingAbstractListeningPropSet<VALUES> extends AbstractListeningPropSet<VALUES> {

    private final PropSet<VALUES> propSet;

    public DelegatingAbstractListeningPropSet(PropSet<VALUES> propSet) {
        this.propSet = propSet;
    }

    @Override
    public Set<String> propKeys() {
        return propSet.propKeys();
    }

    @Override
    public VALUES getVals(Props props) {
        return propSet.getVals(props);
    }

    @Override
    public void setVals(Props props) {
        propSet.setVals(props);
    }
}
