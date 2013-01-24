package com.github.dirkraft.propslive.set.ease;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.set.PropSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Convenience on a {@link PropSet} for a 2-tuple of string props - simply, a pair of properties.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropSetAsPair implements PropSet<Pair<String, String>> {

    /** prop key of the left value of the pair */
    public final String leftKey;
    /** prop key of the right value of the pair */
    public final String rightKey;

    /** when {@link #setVals(Props)}, set this val for the {@link #leftKey} */
    public String leftVal;
    /** when {@link #setVals(Props)}, set this val for the {@link #rightKey} */
    public String rightVal;

    public PropSetAsPair(String leftKey, String rightKey) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    @Override
    public Set<String> propKeys() {
        return new HashSet<>(Arrays.asList(leftKey, rightKey));
    }

    @Override
    public Pair<String, String> getVals(Props props) {
        return Pair.of(props.getString(leftKey), props.getString(rightKey));
    }

    @Override
    public void setVals(Props props) {
        props.setString(leftKey, leftVal);
        props.setString(rightKey, rightVal);
    }
}
