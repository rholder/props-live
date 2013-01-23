package com.github.dirkraft.propslive.set;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.PropsImpl;
import com.github.dirkraft.propslive.dynamic.DynamicProps;
import com.github.dirkraft.propslive.propsrc.PropertySource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Impl of {@link PropsSets}, a component of {@link DynamicProps} for atomically reading or writing sets of related
 * properties.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsSetsImpl extends PropsImpl implements PropsSets {

    /**
     * For {@link PropsSets} method access to properties to restrict them to only those they declare in their
     * {@link PropSet#propKeys()}.
     */
    private static final ThreadLocal<Set<String>> propRestrictions = new ThreadLocal<>();

    /**
     * Restricts access to {@link DynamicProps#impl} by that set in {@link #propRestrictions}, and only prop getters
     * may be used.
     */
    private final Props restrictedGetterProxy = makeRestrictedProxy("^get.*");

    /**
     * Restricts access to {@link DynamicProps#impl} by that set in {@link #propRestrictions}, and only prop setters
     * may be used.
     */
    private final Props restrictedProxy = makeRestrictedProxy("^(get|set).*");

    /**
     * Backed by that of {@link PropsImpl#PropsImpl()}
     */
    public PropsSetsImpl() {
        super();
    }

    /**
     * Backed by arbitrary PropertySource
     *
     * @param source of props
     */
    public PropsSetsImpl(PropertySource source) {
        super(source);
    }

    @Override
    public <VALUES> VALUES getVals(PropSet<VALUES> propSet) {
        // Restrict property access to the PropSet to those declared by its PropSet.propKeys()
        propRestrictions.set(propSet.propKeys());

        try {
            return propSet.getVals(restrictedGetterProxy);
        } finally {
            propRestrictions.remove();
        }
    }

    @Override
    public void setVals(PropSet<?> propSet) {
        // Restrict property access to the PropSet to those declared by its PropSet.propKeys()
        propRestrictions.set(propSet.propKeys());

        try {
            propSet.setVals(restrictedProxy);
        } finally {
            propRestrictions.remove();
        }
    }

    /**
     * @param methodNamePattern that must match for allowed method access
     * @return proxy that restricts to the method prefix and access to props in {@link #propRestrictions}
     */
    private Props makeRestrictedProxy(final String methodNamePattern) {
        // Important, notice that the source is proxied and not the outer PropsImpl itself.
        return new PropsImpl((Props) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Props.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (!method.getName().matches(methodNamePattern)) {
                    throw new IllegalPropertyAccessException("PropSet attempted to access Props method other than what " +
                            "this one strictly provides access to which is '" + methodNamePattern + "'s");
                } else {
                    String propKey = (String) args[0];
                    if (!propRestrictions.get().contains(propKey)) {
                        throw new IllegalPropertyAccessException("PropSet attempted to access property '" + propKey +
                                "' that was not in its declared PropSet.propKeys(): " + propRestrictions.get());
                    }
                }
                // pass through containing PropsSetsImpl for access
                return method.invoke(PropsSetsImpl.this, args);
            }
        }));
    }
}
