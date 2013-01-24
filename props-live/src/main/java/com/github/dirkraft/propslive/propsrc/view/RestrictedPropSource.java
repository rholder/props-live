package com.github.dirkraft.propslive.propsrc.view;

import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.set.IllegalPropertyAccessException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Property source that restricts access to a specific set of properties. Illegal accesses will result in a
 * {@link IllegalPropertyAccessException}.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class RestrictedPropSource implements PropSource {

    private final static String MSG_EXCEPT_RESTRICTED_FMT = "PropertySource is restricted, denied access to '%s'. " +
            "Allowed keys are %s";

    private final PropSource delegate;
    private final Set<String> propKeys;

    public RestrictedPropSource(PropSource propSource, String... propKeys) {
        this(propSource, Arrays.asList(propKeys));
    }

    public RestrictedPropSource(PropSource propSource, Collection<String> propKeys) {
        this(propSource, new HashSet<>(propKeys));
    }

    public RestrictedPropSource(PropSource propSource, Set<String> propKeys) {
        this.delegate = propSource;
        this.propKeys = Collections.unmodifiableSet(propKeys);
    }

    @Override
    public String description() {
        return delegate.description();
    }

    /**
     * @throws IllegalPropertyAccessException if key is not in the allowed set
     */
    @Override
    public String getString(String key) throws IllegalPropertyAccessException {
        if (!propKeys.contains(key)) {
            throw new IllegalPropertyAccessException(String.format(MSG_EXCEPT_RESTRICTED_FMT, key, propKeys));
        } else {
            return delegate.getString(key);
        }
    }

    /**
     * @throws IllegalPropertyAccessException if key is not in the allowed set
     */
    @Override
    public void setString(String key, String value) throws IllegalPropertyAccessException {
        if (!propKeys.contains(key)) {
            throw new IllegalPropertyAccessException(String.format(MSG_EXCEPT_RESTRICTED_FMT, key, propKeys));
        } else {
            delegate.setString(key, value);
        }
    }
}
