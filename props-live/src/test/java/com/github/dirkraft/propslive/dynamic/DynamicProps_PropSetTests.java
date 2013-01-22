package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.Props;
import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicProps_PropSetTests {

    DynamicProps $ = new DynamicProps(new PropertySourceMap(DynamicProps_PropSetTests.class.getName()));

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
class BadTestPropSet implements PropSet<Object> {

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