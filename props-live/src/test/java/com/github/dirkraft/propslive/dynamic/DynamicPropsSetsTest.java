package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.dynamic.listen.PropChange;
import com.github.dirkraft.propslive.propsrc.PropSourceMap;
import com.github.dirkraft.propslive.set.IllegalPropertyAccessException;
import com.github.dirkraft.propslive.set.ease.DelegatingAbstractListeningPropSet;
import com.github.dirkraft.propslive.core.LivePropSet;
import com.github.dirkraft.propslive.set.ease.PropSetAsPair;
import com.github.dirkraft.propslive.set.ease.PropSetAsPropSlice;
import com.github.dirkraft.propslive.set.ease.PropsSlice;
import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        LivePropSet propSetAndListenerAB = new LivePropSet("test.a", "test.b") {
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
        LivePropSet propSetAndListenerABC = new LivePropSet("test.a", "test.b", "test.c") {
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

        // Read into ABC. UberPropSet is a Props which writes into itself on getVals so the thing returned by
        // getVals(UberPropSet) is actually the UberPropSet itself. Therefore, no need to capture the return value.
        $.getVals(propSetAndListenerABC);
        Assert.assertEquals("Should have overwritten values originally set in ABC", "Astring", propSetAndListenerABC.getString("test.a"));
        Assert.assertEquals("Should have overwritten values originally set in ABC", "Bstring", propSetAndListenerABC.getString("test.b"));
        Assert.assertEquals("Should have overwritten values originally set in ABC", null, propSetAndListenerABC.getString("test.c"));
        // no change in fire count
        Assert.assertEquals(1, reloadCountAB.value.intValue());
        Assert.assertEquals(0, reloadCountABC.value.intValue());

        propSetAndListenerAB.setString("test.a", "FinalA");
        propSetAndListenerAB.setString("test.b", "FinalB");
        try {
            propSetAndListenerAB.setString("test.c", "FinalC");
            Assert.fail("ab is not allowed to write on c, and so that write should not even be staged");
        } catch (IllegalPropertyAccessException e) {
            // expected
        }

        $.setVals(propSetAndListenerAB);
        Assert.assertEquals("FinalA", $.getString("test.a"));
        Assert.assertEquals("FinalB", $.getString("test.b"));
        Assert.assertEquals(null, $.getString("test.c"));

        Assert.assertEquals(2, reloadCountAB.value.intValue());
        Assert.assertEquals("ABC is still not subscribed to change events", 0, reloadCountABC.value.intValue());

        // subscribe ABC
        $.to(propSetAndListenerABC).getVals(propSetAndListenerABC);
        $.setString("test.c", "haxed C");
        Assert.assertEquals(2, reloadCountAB.value.intValue());
        Assert.assertEquals(1, reloadCountABC.value.intValue());

        // change all of them
        propSetAndListenerABC.setString("test.a", "ABC's A");
        propSetAndListenerABC.setString("test.b", "ABC's B");
        propSetAndListenerABC.setString("test.c", "ABC's C");
        $.setVals(propSetAndListenerABC);

        // each fired once, even though all of their properties changed
        Assert.assertEquals(3, reloadCountAB.value.intValue());
        Assert.assertEquals(2, reloadCountABC.value.intValue());

        // each fired once, even though one of their properties changes
        $.setString("test.b", "haxed B");
        Assert.assertEquals(4, reloadCountAB.value.intValue());
        Assert.assertEquals(3, reloadCountABC.value.intValue());

        final Holder<Integer> reloadCountBCD = new Holder<>(0);
        LivePropSet propSetAndListenerBCD = new LivePropSet("test.b", "test.c", "test.d") {
            {
                setString("test.b", "B is in the house");
                setString("test.c", "C is in the house");
                setString("test.d", "D is in the house");
            }

            @Override
            public void reload(PropChange<PropsSlice> values) {
                ++reloadCountBCD.value;
            }
        };

        // affects both the AB and ABC listeners (due to BC change)
        $.setVals(propSetAndListenerBCD);
        Assert.assertEquals(5, reloadCountAB.value.intValue());
        Assert.assertEquals(4, reloadCountABC.value.intValue());

        // Register BCD against only one property and one that it is NOT allowed to read! This is very weird, and should
        // probably rarely actually be done. But this is a test, and is a supported use.
        $.to(propSetAndListenerBCD).getString("test.a");
        $.setString("test.b", "random B");
        Assert.assertEquals("Should not have fired. BCD is actually only registered to test.a changes.", 0, reloadCountBCD.value.intValue());
        Assert.assertEquals(6, reloadCountAB.value.intValue());
        Assert.assertEquals(5, reloadCountABC.value.intValue());

        // A little more reasonable, register to a prop that's in its propset.
        $.to(propSetAndListenerBCD).getString("test.c");
        $.setString("test.c", "random C");
        Assert.assertEquals(6, reloadCountAB.value.intValue());
        Assert.assertEquals(6, reloadCountABC.value.intValue());
        Assert.assertEquals(1, reloadCountBCD.value.intValue());
    }

    @Test(timeout = 30 * 1000) // 30 seconds
    public void testThreadsDisjointWriters() throws InterruptedException {
        final LivePropSet ab = new LivePropSet("test.a", "test.b") {
            @Override
            public void reload(PropChange<PropsSlice> values) {
            }
        };
        final LivePropSet de = new LivePropSet("test.d", "test.e") {
            @Override
            public void reload(PropChange<PropsSlice> values) {
            }
        };

        // register each to all of their props
        $.to(ab).getVals(ab);
        $.to(de).getVals(de);

        final Holder<Boolean> exceptionOccurred = new Holder<>(false);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        { // randomizes values in abc and writes them
            final long randomSeed = System.currentTimeMillis();
            executorService.submit(new Runnable() {
                Random r = new Random(randomSeed);
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 32768 && !exceptionOccurred.value; ++i) {
                            ab.setInt("test.a", r.nextInt());
                            ab.setInt("test.b", r.nextInt());
                            $.setVals(ab);
                        }
                    } catch (PropLockingException e) {
                        exceptionOccurred.value = true;
                    }
                }
            });
        }
        { // randomizes values in abc and writes them
            final long randomSeed = System.currentTimeMillis() % 123456;
            executorService.submit(new Runnable() {
                Random r = new Random(randomSeed);
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 32768 && !exceptionOccurred.value; ++i) {
                            de.setInt("test.d", r.nextInt());
                            de.setInt("test.e", r.nextInt());
                            $.setVals(de);
                        }
                    } catch (PropLockingException e) {
                        exceptionOccurred.value = true;
                    }
                }
            });
        }
        executorService.shutdown();
        Assert.assertTrue(executorService.awaitTermination(30, TimeUnit.SECONDS));
        Assert.assertFalse(exceptionOccurred.value);
    }

    @Test(timeout = 30 * 1000) // 30 seconds
    public void testThreadsOverlappingWritersException() throws InterruptedException {
        final LivePropSet abc = new LivePropSet("test.a", "test.b", "test.c") {
            @Override
            public void reload(PropChange<PropsSlice> values) {
            }
        };
        final LivePropSet cde = new LivePropSet("test.c", "test.d", "test.e") {
            @Override
            public void reload(PropChange<PropsSlice> values) {
            }
        };

        // register each to all of their props
        $.to(abc).getVals(abc);
        $.to(cde).getVals(cde);

        final Holder<Boolean> exceptionOccurred = new Holder<>(false);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        { // randomizes values in abc and writes them
            final long randomSeed = System.currentTimeMillis();
            executorService.submit(new Runnable() {
                Random r = new Random(randomSeed);
                @Override
                public void run() {
                    try {
                        while (!exceptionOccurred.value) {
                            abc.setInt("test.a", r.nextInt());
                            abc.setInt("test.b", r.nextInt());
                            abc.setInt("test.c", r.nextInt());// this one would cause the write lock exception
                            $.setVals(abc);
                        }
                    } catch (PropLockingException e) {
                        exceptionOccurred.value = true;
                    }
                }
            });
        }
        { // randomizes values in abc and writes them
            final long randomSeed = System.currentTimeMillis() % 123456;
            executorService.submit(new Runnable() {
                Random r = new Random(randomSeed);
                @Override
                public void run() {
                    try {
                        while (!exceptionOccurred.value) {
                            cde.setInt("test.c", r.nextInt());// this one would cause the write lock exception
                            cde.setInt("test.d", r.nextInt());
                            cde.setInt("test.e", r.nextInt());
                            $.setVals(cde);
                        }
                    } catch (PropLockingException e) {
                        exceptionOccurred.value = true;
                    }
                }
            });
        }
        executorService.shutdown();
        Assert.assertTrue(executorService.awaitTermination(30, TimeUnit.SECONDS));
        Assert.assertTrue(exceptionOccurred.value);
    }
}
