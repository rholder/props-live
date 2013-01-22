package com.github.dirkraft.propslive.view;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.propsrc.PropertySource;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Extension of {@link PropsImpl} but can accept an override 'layer' of {@link Props}. Lookups will iterate through
 * all provided property sources until {@link #isSet(String)} is true.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class LayeredPropertySource implements PropertySource {

    private final List<PropertySource> propertySources;

    /**
     * @param propertySources in order of decreasing precedence (iteration order)
     */
    public LayeredPropertySource(PropertySource... propertySources) {
        this(Arrays.asList(propertySources));
    }

    /**
     * @param propertySources in order of decreasing precedence (iteration order)
     */
    public LayeredPropertySource(List<PropertySource> propertySources) {
        this.propertySources = propertySources;
    }

    @Override
    public String description() {
        return getClass().getName() + propertySources;
    }

    /**
     * Iterates through the {@link PropertySource}s given via the constructor until a value that satisfies
     * {@link #isSet(String)} is found. If no such value satisfies the condition, returns <code>null</code>
     *
     * @return first satisfying value, or otherwise null
     */
    @Override
    public String getString(String propKey) {
        Iterator<PropertySource> it = propertySources.iterator();
        String propVal = null;
        while (it.hasNext() && !isSet(propVal = it.next().getString(propKey))) ;
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
        throw new UnsupportedOperationException("Changing prop vals on a " + getClass().getName() + " is not possible.");
    }
}
