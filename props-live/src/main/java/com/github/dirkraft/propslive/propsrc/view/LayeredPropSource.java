package com.github.dirkraft.propslive.propsrc.view;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.propsrc.PropSource;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Extension of {@link PropsImpl} but can accept an override 'layer' of {@link Props}. Lookups will iterate through
 * all provided property sources until {@link #isSet(String)} is true.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class LayeredPropSource implements PropSource {

    /** {@link #setString(String, String)} are not applied to the underlying {@link #propSources} and instead to this. */
    private final PropSource writeReceivingPropSource;
    private final List<PropSource> propSources;

    /**
     * @param propSources in order of decreasing precedence (iteration order)
     */
    public LayeredPropSource(PropSource... propSources) {
        this(Arrays.asList(propSources));
    }

    /**
     * @param propSources in order of decreasing precedence (iteration order)
     */
    public LayeredPropSource(List<PropSource> propSources) {
        writeReceivingPropSource = new PropSourceMap("write-catcher created by " + getClass().getSimpleName());
        ArrayList<PropSource> prependedPropSources = new ArrayList<PropSource>(propSources.size() + 1);
        prependedPropSources.add(writeReceivingPropSource);
        prependedPropSources.addAll(propSources);
        this.propSources = prependedPropSources;
    }

    @Override
    public String description() {
        return getClass().getName() + propSources;
    }

    /**
     * Iterates through the {@link PropSource}s given via the constructor until a value that satisfies
     * {@link #isSet(String)} is found. If no such value satisfies the condition, returns <code>null</code>
     *
     * @return first satisfying value, or otherwise null
     */
    @Override
    public String getString(String propKey) {
        Iterator<PropSource> it = propSources.iterator();
        String propVal = null;
        while (it.hasNext() && !isSet(propVal = it.next().getString(propKey)));
        return propVal;
    }

    /**
     * The default implementation uses {@link StringUtils#isBlank(CharSequence)}.
     * <pre>
     * isSet == !StringUtils.isBlank(propVal)
     * </pre>
     * This implicates that even if a property is explicitly set to <code>null</code>, it will be interpreted
     * as not set at all {@link #getString(String)} will not stop and return that null.
     *
     * @param propVal to test
     * @return whether or not the given propVal should be considered a specified value
     */
    protected boolean isSet(String propVal) {
        return !StringUtils.isBlank(propVal);
    }

    @Override
    public void setString(String key, String value) {
        writeReceivingPropSource.setString(key, value);
    }

    /**
     * @return A merged view of the underlying property sources with the earliest property sources 'winning' the value
     *         for contested keys. Changes to this map will not affect any of the original prop sources in this
     *         LayeredPropSource.
     */
    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<String, String>();
        // put values in reverse order so that earlier prop sources will 'win', overwrite later prop sources
        for (int i = propSources.size() - 1; i >= 0; --i) {
            PropSource propSource = propSources.get(i);
            map.putAll(propSource.asMap());
        }
        return map;
    }
}
