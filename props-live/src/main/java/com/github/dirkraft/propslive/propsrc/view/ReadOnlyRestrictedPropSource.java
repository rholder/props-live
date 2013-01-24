package com.github.dirkraft.propslive.propsrc.view;

import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.set.IllegalPropertyAccessException;

import java.util.Collection;
import java.util.Set;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class ReadOnlyRestrictedPropSource extends RestrictedPropSource {
    public ReadOnlyRestrictedPropSource(PropSource propSource, String... propKeys) {
        super(propSource, propKeys);
    }

    public ReadOnlyRestrictedPropSource(PropSource propSource, Collection<String> propKeys) {
        super(propSource, propKeys);
    }

    public ReadOnlyRestrictedPropSource(PropSource propSource, Set<String> propKeys) {
        super(propSource, propKeys);
    }

    /**
     * @throws IllegalPropertyAccessException if attempted, as this is a read-only PropSource
     */
    @Override
    public void setString(String key, String value) throws IllegalPropertyAccessException {
        throw new IllegalPropertyAccessException("Writes are not permissible in this read-only context.");
    }
}
