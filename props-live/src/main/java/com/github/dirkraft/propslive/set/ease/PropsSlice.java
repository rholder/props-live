package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.propsrc.view.RestrictedPropSource;
import com.github.dirkraft.propslive.set.IllegalPropertyAccessException;
import com.github.dirkraft.propslive.set.PropsSets;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is actually equivalent to {@link PropsImpl}, but this different name is provided in contexts where it makes
 * sense to deal with some smaller subset of a larger total set of properties (as in the case of {@link PropsSets}
 * (see {@link PropSetAsPropSlice}).
 * <p/>
 * This instance will never be backed by an externally-provided PropSource. Constructors that accept an external
 * prop source will clone values from them. So changing prop vals on this will not affect those external PropSources.
 * <p/>
 * Note that prop keys of interest must be declared up front, as this is backed by a {@link RestrictedPropSource}.
 * Later attempts to get/set undeclared prop keys will result in {@link IllegalPropertyAccessException}s.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsSlice extends PropsImpl {

    /**
     * Inits against propKeys with null values.
     */
    public PropsSlice(String... propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropsSlice(String...)"), propKeys);
    }

    /**
     * Inits against propKeys with null values.
     */
    public PropsSlice(Collection<String> propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropsSlice(Collection<String>)"), propKeys);
    }

    /**
     * Inits against propKeys with null values.
     */
    public PropsSlice(Set<String> propKeys) {
        this(new PropSourceMap("empty PropSourceMap created by PropsSlice(Set<String>)"), propKeys);
    }

    /**
     * @param source to clone vals from
     */
    public PropsSlice(PropSource source, String... propKeys) {
        this(source, Arrays.asList(propKeys));
    }

    /**
     * @param source to clone vals from
     */
    public PropsSlice(final PropSource source, final Collection<String> propKeys) {
        this(source, new HashSet<>(propKeys));
    }

    /**
     * @param source to clone vals from
     */
    public PropsSlice(final PropSource source, final Set<String> propKeys) {
        super(new RestrictedPropSource(new PropSourceMap() {{
            for (String propKey : propKeys) {
                setString(propKey, source.getString(propKey));
            }
        }}, propKeys));
    }
}
