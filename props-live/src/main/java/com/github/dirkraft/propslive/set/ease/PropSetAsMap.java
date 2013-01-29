package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.dynamic.DynamicPropsSets;
import com.github.dirkraft.propslive.set.PropSet;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract impl of {@link PropSet} with conveniences like that of a map where all values are in the form of
 * Strings, and helper methods to chain in default values for {@link #getVals(Props)} or values to write for
 * {@link #setVals(Props)}.
 * <hr/>
 *
 * In the following examples 'propsSetsImpl' is your principal property lookup object (e.g. could be
 * {@link DynamicPropsSets})
 * <p/>
 *
 * For simply reading a set of props into a Map
 * <pre>
 * PropSetMap propSet = new PropSetMap("prop.one", "prop.two", "prop.three");
 * Map<String, String> propsAsStrings = propsSetsImpl.getVals(propSet);
 * </pre>
 *
 * For reading with defaults and writing sets of props
 * <pre>
 * PropSetMap propSet = new PropSetMap("prop.one", "prop.two", "prop.three").withDefaults("1", "2", "3");
 * Map<String, String> propsAsStrings = propsSetsImpl.getVals(propSet);
 * // and to write values
 * propSet.withWrites("4", "5", "6");
 * propsSetsImpl.setVals(propSet);
 * </pre>
 *
 * Not all properties must be written and can be skipped - all prop writes are skipped by default until you override
 * that via {@link #withWrites(String...)}.
 * <pre>
 * PropSetMap propSet = new PropSetMap("prop.one", "prop.two", "prop.three").withWrites("4", SKIP_WRITE, "6");
 * propsSetsImpl.setVals(propSet);
 * // This will actually write null into "prop.two"
 * propSet.withWrites(SKIP_WRITE, null, SKIP_WRITE);
 * propsSetsImpl.setVals(propSet);
 * </pre>
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSetAsMap implements PropSet<Map<String, String>> {

    /**
     * Special value token that can be passed to {@link #withWrites(String...)} that will skip writing that property
     * entirely.
     */
    public static final String SKIP_WRITE = new String(""); // new String is crucial to making this a special instance

    final List<Pair<String, String>> propDefaults;
    final List<Pair<String, String>> propWrites;

    /**
     * @param propKeys to register to {@link #propKeys()}
     */
    public PropSetAsMap(String... propKeys) {
        this(Arrays.asList(propKeys));
    }

    /**
     * @param propKeys to register to {@link #propKeys()}
     */
    public PropSetAsMap(List<String> propKeys) {
        this.propDefaults = new ArrayList<>(propKeys.size());
        this.propWrites = new ArrayList<>(propKeys.size());
        for (String propKey : propKeys) {
            this.propDefaults.add(MutablePair.of(propKey, (String) null));
            this.propWrites.add(MutablePair.of(propKey, SKIP_WRITE)); // default to not changing properties
        }
    }

    /**
     * A constructor that prepares this PropSetAsMap based off a map of props. Stages both defaults and writes so that
     * this instance can be effectively used for either.
     *
     * @param props to whose entries will be the {@link #propKeys()} of this prop set, and whose values will be BOTH
     *              staged defaultVals and staged writes.
     */
    public PropSetAsMap(Map<String, String> props) {
        this.propDefaults = new ArrayList<>(props.size());
        this.propWrites = new ArrayList<>(props.size());
        for (Map.Entry<String, String> entry : props.entrySet()) {
            this.propDefaults.add(MutablePair.of(entry.getKey(), entry.getValue()));
            this.propWrites.add(MutablePair.of(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Sets default values to use when doing {@link #getVals(Props)}.
     * <p/>
     * See {@link PropSetAsMap} for example chaining usage.
     *
     * @param defaultVals corresponding to constructor order of default values
     * @return this for chaining
     * @throws RuntimeException if params length does not match that of {@link #propKeys()}
     */
    public PropSetAsMap withDefaults(String... defaultVals) throws RuntimeException {
        stageVals(this.propDefaults, defaultVals);
        return this;
    }

    /**
     * Prepares values that should be written when doing {@link #setVals(Props)}. Use {@link #SKIP_WRITE} to skip
     * writing anything to some property (whereas specifying <code>null</code> would explicitly null out that property).
     * <p/>
     * See {@link PropSetAsMap} for example chaining usage
     *
     * @param vals corresponding to constructor order of values to set
     * @return this for chaining
     * @throws RuntimeException if params length does not match that of {@link #propKeys()}
     */
    public PropSetAsMap withWrites(String... vals) throws RuntimeException {
        stageVals(this.propWrites, vals);
        return this;
    }

    private void stageVals(List<Pair<String, String>> props, String... vals) {
        if (props.size() != vals.length) {
            throw new RuntimeException("Given params length must match that of this.propKeys()");
        }
        for (int i = 0; i < props.size(); ++i) {
            Pair<String, String> prop = props.get(i);
            prop.setValue(vals[i]);
        }
    }

    @Override
    public Set<String> propKeys() {
        Set<String> propKeys = new HashSet<>(propDefaults.size());
        for (Pair<String, String> propDefault : propDefaults) {
            propKeys.add(propDefault.getKey());
        }
        return propKeys;
    }

    /**
     * @return map of prop keys to string forms of their values, defaulting to those specified by
     *         {@link #withDefaults(String...)}
     */
    @Override
    public Map<String, String> getVals(Props props) {
        Map<String, String> vals = new HashMap<>(this.propDefaults.size());
        for (Pair<String, String> propDefault : this.propDefaults) {
            String propKey = propDefault.getKey();
            vals.put(propKey, props.getString(propKey, propDefault.getValue()));
        }
        return vals;
    }

    /**
     * set values on given 'props' with values specified by {@link #withWrites(String...)} where {@link #SKIP_WRITE}
     * values will bypass attempting any write on that prop key
     */
    @Override
    public void setVals(Props props) {
        for (Pair<String, String> propWrite : this.propWrites) {
            String value = propWrite.getValue();
            if (SKIP_WRITE != value) {
                props.setString(propWrite.getKey(), value);
            }
        }
    }
}
