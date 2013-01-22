package com.github.dirkraft.propslive.dynamic;

import com.github.dirkraft.propslive.propsrc.PropertySourceMap;
import junit.framework.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class DynamicPropsTest {

    final DynamicProps $ = new DynamicProps(new PropertySourceMap(DynamicPropsTest.class.getName()));

    Holder<Integer> triggeredReload = new Holder<>(0);
    PropListener<Boolean> listener = new PropListener<Boolean>() {
        @Override
        public void reload(PropChange<Boolean> values) {
            ++triggeredReload.value;
        }
    };

    @Test
    public void testNoRegistration() {
        Boolean bool = $.getBool("test.bool");
        Assert.assertNull(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        $.setBool("test.bool", false);
        bool = $.getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());
    }

    @Test
    public void testRegistration() {
        $.setBool("test.bool", false);

        Boolean bool = $.to(listener).getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        bool = $.to(listener).getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals("now that it's registered make sure any get doesn't trigger reload",
                0, triggeredReload.value.intValue());
        bool = $.getBool("test.bool");
        Assert.assertFalse(bool);
        Assert.assertEquals(0, triggeredReload.value.intValue());

        $.setBool("test.bool", false);
        Assert.assertEquals("setting to same as previous should not trigger reload",
                0, triggeredReload.value.intValue());
        $.setBool("test.bool", true);
        Assert.assertEquals("setting to a different value should trigger reload",
                1, triggeredReload.value.intValue());

        $.setBool("another.bool", false);
        Assert.assertEquals("setting some other thing to a different value should NOT trigger reload",
                1, triggeredReload.value.intValue());
    }

    @Test
    public void testThreadsReadersAndWrite() throws InterruptedException {
        $.setInt("test.int", 0);
        ExecutorService executorService = Executors.newFixedThreadPool(64);
        final AtomicBoolean exception = new AtomicBoolean();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                int iterations = 4 * 32768; // some attempt to be running the whole time the readers are
                for (int i = 0; i < iterations; ++i) {
                    $.setInt("test.int", 1 + $.getInt("test.int"));
                }
            }
        });
        for (int i = 0; i < 63; i++) {
             executorService.submit(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         int last = 0;
                         // essentially expecting neither exceptions nor backwards time
                         for (int j = 0; j < 32768; ++j) {
                             if (j % 32 == 0) { // should cause threads to shuffle around a bit more
                                 Thread.sleep(1L);
                             }
                             int current = $.getInt("test.int");
                             Assert.assertTrue(current >= last);
                             last = current;
                         }
                     } catch (Exception e) {
                         exception.set(true);
                     }
                 }
             });
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        // Concurrent reads are allowed along with a single write. There should have been no exceptions.
        Assert.assertFalse(exception.get());
    }

    @Test
    public void testThreadsWritersException() throws InterruptedException {
        $.setInt("test.int", 0);
        ExecutorService executorService = Executors.newFixedThreadPool(64);
        final AtomicBoolean exception = new AtomicBoolean();
        for (int i = 0; i < 64; ++i) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 32768; ++j) {
                            if (j % 32 == 0) { // should cause threads to shuffle around a little bit more
                                Thread.sleep(1L);
                            }
                            $.setInt("test.int", j);
                        }
                    } catch (PropLockingException e) {
                        if (e.getMessage().startsWith("Failed to acquire write lock for prop test.int")) {
                            exception.set(true);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        // Concurrent writes is not allowed and should result in exceptions.
        Assert.assertTrue(exception.get());
    }

    @Test
    public void testThreadsListeners() throws InterruptedException {
        $.setInt("test.int", 0);

        final AtomicInteger reloadCount = new AtomicInteger();
        List<PropListener<?>> listeners = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            PropListener<Integer> listener = new PropListener<Integer>() {
                @Override
                public void reload(PropChange<Integer> values) {
                    reloadCount.getAndIncrement();
                }
            };
            // Register as listeners on props.
            int wouldHaveDoneInitWithThisExceptThisIsATest = $.to(listener).getInt("test.int");
            listeners.add(listener);
        }

        final AtomicInteger setCount = new AtomicInteger();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final AtomicBoolean exception = new AtomicBoolean();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // essentially expecting neither exceptions nor backwards time
                    for (int j = 0; j < 32768; ++j) {
                        if (j % 32 == 0) { // should cause threads to shuffle around a bit more
                            Thread.sleep(1L);
                        }
                        // triggering listeners, passing in set count value so that it counts as a change every time
                        $.setInt("test.int", setCount.incrementAndGet());
                    }
                } catch (Exception e) {
                    exception.set(true);
                }
            }
        });
        executorService.shutdown();
        executorService.awaitTermination(100, TimeUnit.SECONDS);
        Assert.assertFalse(exception.get());

        Assert.assertEquals("32768 set calls", 32768, setCount.get());
        Assert.assertEquals("64 listeners * 32768 set calls = 2097152 reloads", 2097152, reloadCount.get());
    }
}
