package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import com.github.dirkraft.propslive.set.IllegalPropertyAccessException;
import com.github.dirkraft.propslive.set.PropSet;
import com.github.dirkraft.propslive.set.PropsSets;
import com.github.dirkraft.propslive.set.PropsSetsImpl;
import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropsSetsTests {

    PropsSets $ = new PropsSetsImpl(new PropertySourceMap(PropsSetsTests.class.getName()));

    @Test
    public void testPropSets() {
        final AtomicInteger counter = new AtomicInteger();

        PropSet<Integer> singleIntPropSet = new PropSet<Integer>() {
            @Override
            public LinkedHashSet<String> propKeys() {
                return new LinkedHashSet<>(Arrays.asList("test.key"));
            }

            @Override
            public Integer getVals(Props props) {
                return props.getInt("test.key");
            }

            @Override
            public void setVals(Props props) {
                props.setInt("test.key", counter.getAndIncrement());
            }
        };

        Integer val = $.getInt("test.key");
        Assert.assertEquals(null, val);

        val = $.getVals(singleIntPropSet);
        Assert.assertEquals(null, val);

        $.setVals(singleIntPropSet);
        val = $.getVals(singleIntPropSet);
        Assert.assertEquals(0, val.intValue());

        // more interesting, a prop set
        $.setString("test.s", "boring s");
        $.setString("test.t", "boring t");
        $.setInt("test.i", 25);

        GoodTestPropSet multiPropSet = new GoodTestPropSet();

        GoodTestPropSetVals propSetVals = $.getVals(multiPropSet);
        Assert.assertEquals("boring s", propSetVals.s);
        Assert.assertEquals("boring t", propSetVals.t);
        Assert.assertEquals(25, propSetVals.i.intValue());

        $.setVals(multiPropSet);
        propSetVals = $.getVals(multiPropSet);
        Assert.assertEquals("a new s", propSetVals.s);
        Assert.assertEquals("a different t", propSetVals.t);
        Assert.assertEquals(75, propSetVals.i.intValue());
    }

    @Test
    public void testDisjointPropSets() {
        $.setString("test.a", "a");
        $.setString("test.b", "b");
        $.setString("test.c", "c");
        $.setString("test.d", "d");

        PairPropSet ab = new PairPropSet("test.a", "test.b");
        PairPropSet cd = new PairPropSet("test.c", "test.d");

        Pair<String, String> vals = $.getVals(ab);
        Assert.assertEquals("a", vals.getLeft());
        Assert.assertEquals("b", vals.getRight());
        vals = $.getVals(cd);
        Assert.assertEquals("c", vals.getLeft());
        Assert.assertEquals("d", vals.getRight());

        ab.leftVal = "A";
        ab.rightVal = "B";
        $.setVals(ab);

        vals = $.getVals(ab);
        Assert.assertEquals("A", vals.getLeft());
        Assert.assertEquals("B", vals.getRight());
        vals = $.getVals(cd);
        Assert.assertEquals("c", vals.getLeft());
        Assert.assertEquals("d", vals.getRight());

        cd.leftVal = "C";
        cd.rightVal = "D";
        $.setVals(cd);

        vals = $.getVals(ab);
        Assert.assertEquals("A", vals.getLeft());
        Assert.assertEquals("B", vals.getRight());
        vals = $.getVals(cd);
        Assert.assertEquals("C", vals.getLeft());
        Assert.assertEquals("D", vals.getRight());
    }

    @Test
    public void testIntersectingPropSets() {
        $.setString("test.a", "a");
        $.setString("test.b", "b");
        $.setString("test.c", "c");

        PairPropSet ab = new PairPropSet("test.a", "test.b");
        PairPropSet bc = new PairPropSet("test.b", "test.c");

        Pair<String, String> vals = $.getVals(ab);
        Assert.assertEquals("a", vals.getLeft());
        Assert.assertEquals("b", vals.getRight());
        vals = $.getVals(bc);
        Assert.assertEquals("b", vals.getLeft());
        Assert.assertEquals("c", vals.getRight());

        ab.leftVal = "A";
        ab.rightVal = "B";
        $.setVals(ab);

        vals = $.getVals(ab);
        Assert.assertEquals("A", vals.getLeft());
        Assert.assertEquals("B", vals.getRight());
        vals = $.getVals(bc);
        Assert.assertEquals("B", vals.getLeft());
        Assert.assertEquals("c", vals.getRight());

        bc.leftVal = "aieeeeee";
        bc.rightVal = "C";
        $.setVals(bc);

        vals = $.getVals(ab);
        Assert.assertEquals("A", vals.getLeft());
        Assert.assertEquals("aieeeeee", vals.getRight());
        vals = $.getVals(bc);
        Assert.assertEquals("aieeeeee", vals.getLeft());
        Assert.assertEquals("C", vals.getRight());
    }

    @Test
    public void testIllegalPropAccess() {
        BadAccessPropSet badAccessPropSet = new BadAccessPropSet();
        try {
            $.getVals(badAccessPropSet);
            Assert.fail("Expected IllegalPropertyAccessException");
        } catch (IllegalPropertyAccessException e) {
            // good
        }

        try {
            $.setVals(badAccessPropSet);
            Assert.fail("Expected IllegalPropertyAccessException");
        } catch (IllegalPropertyAccessException e) {
            // good
        }

        BadImplPropSet badImplPropSet = new BadImplPropSet();
        try {
            $.getVals(badImplPropSet);
            Assert.fail("Expected IllegalPropertyAccessException");
        } catch (IllegalPropertyAccessException e) {
            // good
        }
    }
}

class GoodTestPropSetVals {
    String s;
    String t;
    Integer i;
}

class GoodTestPropSet implements PropSet<GoodTestPropSetVals> {

    @Override
    public LinkedHashSet<String> propKeys() {
        return new LinkedHashSet<>(Arrays.asList("test.s", "test.t", "test.i"));
    }

    @Override
    public GoodTestPropSetVals getVals(Props props) {
        GoodTestPropSetVals vals = new GoodTestPropSetVals();
        vals.s = props.getString("test.s");
        vals.t = props.getString("test.t");
        vals.i = props.getInt("test.i");
        return vals;
    }

    @Override
    public void setVals(Props props) {
        props.setString("test.s", "a new s");
        props.setString("test.t", "a different t");
        props.setInt("test.i", 75);
    }
}

/**
 * This impl attempts to access properties not declared in its {@link #propKeys()}
 */
class BadAccessPropSet implements PropSet<Object> {

    @Override
    public LinkedHashSet<String> propKeys() {
        return new LinkedHashSet<>(Arrays.asList("test.prop"));
    }

    @Override
    public Object getVals(Props props) {
        props.getString("test.prop");
        props.getString("something.else"); // should except
        return null;
    }

    @Override
    public void setVals(Props props) {
        props.setString("test.prop", "okay");
        props.setString("something.else", "should cause exception"); // should except
    }
}

class BadImplPropSet implements PropSet<Object> {

    @Override
    public LinkedHashSet<String> propKeys() {
        return new LinkedHashSet<>(Arrays.asList("test.prop"));
    }

    @Override
    public Object getVals(Props props) {
        // *snicker* "I will hax all your props in the getter!" - This should result in an exception.
        props.setString("test.prop", "database://full.of.sql.injections");
        return null;
    }

    @Override
    public void setVals(Props props) {
        // both gets and sets are allowed here
    }
}

class PairPropSet implements PropSet<Pair<String, String>> {

    /** prop key of the left value of the pair */
    final String leftKey;
    /** prop key of the right value of the pair */
    final String rightKey;

    /** when {@link #setVals(Props)}, set this val for the {@link #leftKey} */
    String leftVal;
    /** when {@link #setVals(Props)}, set this val for the {@link #rightKey} */
    String rightVal;

    PairPropSet(String leftKey, String rightKey) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    @Override
    public LinkedHashSet<String> propKeys() {
        return new LinkedHashSet<>(Arrays.asList(leftKey, rightKey));
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