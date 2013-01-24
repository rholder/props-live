package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.set.ease.DelegatingAbstractListeningPropSet;
import com.github.dirkraft.propslive.set.ease.PropSetAsPair;
import com.github.dirkraft.propslive.set.ease.PropSetAsPropSlice;
import com.github.dirkraft.propslive.set.ease.PropsSlice;
import com.github.dirkraft.propslive.set.ease.UberPropSet;
import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import javax.xml.ws.Holder;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicPropsSetsTest extends DynamicPropsTest<DynamicPropsSets> { // make sure we didn't break anything inherited

    public DynamicPropsSetsTest() {
        super(new DynamicPropsSets(new PropSourceMap(DynamicPropsSetsTest.class.getName())));
    }

    @Test
    public void testGetVals() {
        $.setString("test.prop1", "wheee");
        $.setString("test.prop2", "whooo");

        PropSetAsPair propSetPair = new PropSetAsPair("test.prop1", "test.prop2");

        Pair<String,String> vals = $.getVals(propSetPair);
        Assert.assertEquals("wheee", vals.getLeft());
        Assert.assertEquals("whooo", vals.getRight());
    }

    @Test
    public void testSetVals() {
        $.setString("test.prop1", "wheee");
        $.setString("test.prop2", "whooo");

        PropSetAsPair propSetPair = new PropSetAsPair("test.prop1", "test.prop2");
        propSetPair.leftVal = "abc";
        propSetPair.rightVal = "123";

        $.setVals(propSetPair);
        Assert.assertEquals("abc", $.getString("test.prop1"));
        Assert.assertEquals("123", $.getString("test.prop2"));
    }

    @Test
    public void testPropSetListenersOnSingularPropChanges() {
        final Holder<Integer> reloadCount = new Holder<>(0);
        // look at this beast.
        DelegatingAbstractListeningPropSet<PropsSlice> listener = new DelegatingAbstractListeningPropSet<PropsSlice>(
            // delegate PropSet impl
            new PropSetAsPropSlice("test.long", "test.bool")
        ) {
            // PropSetListener impl
            @Override
            public void reload(PropChange<PropsSlice> values) {
                ++reloadCount.value;
            }
        };

        $.setLong("test.long", 1234L);
        $.setBool("test.bool", false);
        $.setString("test.unrelated", "whatever");

        Assert.assertEquals("Listener should not have fired. It hasn't been registered yet.", 0, reloadCount.value.intValue());

        // registration against props
        $.to(listener).getVals(listener);

        $.setString("test.unrelated", "something diff");
        Assert.assertEquals("Listener should not have fired. It has been registered, but props it cares about have not changed.",
                0, reloadCount.value.intValue());

        $.setLong("test.long", 11000000000L); // eleventy billion
        Assert.assertEquals("Listener should have fired once.", 1, reloadCount.value.intValue());

        $.setBool("test.bool", true); // eleventy billion
        Assert.assertEquals("Listener should have fired once more.", 2, reloadCount.value.intValue());

        $.setBool("test.bool", true);
        Assert.assertEquals("Listener should not have fired because the value did not change", 2, reloadCount.value.intValue());
    }

    @Test
    public void testPropSetListenersOnPropSetChanges() {
        final Holder<Integer> reloadCountAB = new Holder<>(0);
        UberPropSet propSetAndListenerAB = new UberPropSet("test.a", "test.b") {
            {
                setString("test.a", "Astring");
                setString("test.b", "Bstring");
            }

            @Override
            public void reload(PropChange values) {
                ++reloadCountAB.value;
            }
        };

        final Holder<Integer> reloadCountABC = new Holder<>(0);
        UberPropSet propSetAndListenerABC = new UberPropSet("test.a", "test.b", "test.c") {
            {
                setString("test.a", "a-string");
                setString("test.b", "b-string");
                setString("test.c", "c-string");
            }

            @Override
            public void reload(PropChange<PropsSlice> values) {
                ++reloadCountABC.value;
            }
        };

        $.setLong("test.long", 1234L);
        $.setBool("test.bool", false);
        $.setString("test.unrelated", "whatever");

        Assert.assertEquals("Listener should not have fired. It hasn't been registered yet.", 0, reloadCountAB.value.intValue());
        Assert.assertEquals("Listener should not have fired. It hasn't been registered yet.", 0, reloadCountABC.value.intValue());

        // AB registration and prop change
        $.to(propSetAndListenerAB).setVals(propSetAndListenerAB);
        Assert.assertEquals("AB listener should have fired (once despite setting two props)", 1, reloadCountAB.value.intValue());
        Assert.assertEquals("ABC listener should not have fired as it has not been registered", 0, reloadCountABC.value.intValue());

        // TODO jason continue here
    }
}
