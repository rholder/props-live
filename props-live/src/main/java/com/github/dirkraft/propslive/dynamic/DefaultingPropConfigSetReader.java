package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.PropConfig;
import com.github.dirkraft.propslive.PropConfigSetValues;
import com.github.dirkraft.propslive.PropConfigSetsImpl;
import com.github.dirkraft.propslive.PropertySource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Implementors of this class may initialize defaults in the constructor if desired, e.g. {@link #set(String, String)}
 * et. al.
 * <p/>
 * Calls to {@link #getValuesSet(PropConfig)} will first check the given config before falling back to defaults in
 * this DefaultingPropConfigSetReader. The implementor of the corollary method
 * {@link #getValuesSetWithDefaults(PropConfig)} should thus not worry about defaults beyond that which is immediately
 * set up in the constructor.
 * <p/>
 * I.e. don't call prop val getters which have a second default-value argument. There is a sanity check which may
 * throw a RuntimeException if such a thing is attempted.
 *
 * @author jason
 */
public abstract class DefaultingPropConfigSetReader<VALUES extends PropConfigSetValues> extends PropConfigSetsImpl {

    /**
     * @param propertySourceDescription basis of the custom populated properties that will go into this instance
     */
    public DefaultingPropConfigSetReader(String propertySourceDescription) {
        // This property source is of the defaults, NOT the master property config records.
        super(new DefaultsMapPropertySource(propertySourceDescription));
    }

    /**
     * {@link #getValuesSetWithDefaults(PropConfig)} is the implementation corollary to this method.
     *
     * @param propConfig to check first before falling back to this DefaultingPropConfigSetReader's set default prop vals.
     * @return an instance of the VALUES class that wraps up a logical set of property values
     */
    public VALUES getValuesSet(final PropConfig propConfig) {
        PropConfig proxiedForDefaultsFallback = (PropConfig) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PropConfig.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (!method.getName().startsWith("get")) {
                            throw new RuntimeException("This PropConfig impl only works with prop val accessors " +
                                    "(method name starts with 'get').");
                        }
                        if (method.getParameterTypes().length != 1) {
                            throw new RuntimeException("This PropConfig impl modifies defaulting behavior and so " +
                                    "methods with a second default-value argument are not supported.");
                        }
                        Object propVal = method.invoke(propConfig, args);
                        if (propVal == null) {
                            // fallback to defaults
                            propVal = method.invoke(DefaultingPropConfigSetReader.this, args);
                        }
                        return propVal;
                    }
                });
        return getValuesSetWithDefaults(proxiedForDefaultsFallback);
    }

    /**
     * {@link #getValuesSet(PropConfig)} is the <code>public</code> corollary to this method.
     * <p/>
     * Implementor should return a <code>VALUES</code> type object which does it's lookups against the given PropConfig
     * impl. The PropConfig argument implementation automatically falls back to defaults set up in this
     * DefaultingPropConfigSetReader. The implementor of this method should not particularly worry about
     *
     * @param propConfig to perform lookups against
     * @return an instance of the VALUES class that wraps up a logical set of property values
     */
    protected abstract VALUES getValuesSetWithDefaults(PropConfig propConfig);

    /**
     * PropertySource implementation that is a Map, which we can put arbitrary info into. In this case it will hold
     * our default values.
     */
    static class DefaultsMapPropertySource extends HashMap<String, String> implements PropertySource {

        private final String description;

        DefaultsMapPropertySource(String description) {
            this.description = "Defaults: " + description;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public String getProp(String key) {
            return get(key);
        }

        @Override
        public DefaultsMapPropertySource setProp(String key, String value) {
            put(key, value);
            return this;
        }
    }
}
