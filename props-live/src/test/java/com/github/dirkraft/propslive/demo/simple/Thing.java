package com.github.dirkraft.propslive.demo.simple;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.dirkraft.propslive.demo.simple.Config.$;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class Thing {

    public static final String PROP_NUM_THREADS = "thing.num_threads";
    public static final int DEF_NUM_THREADS = 16;

    private final ExecutorService executorService;

    public Thing() {
        executorService = Executors.newFixedThreadPool($.getInt(PROP_NUM_THREADS, DEF_NUM_THREADS));
    }

    public void start() {
        // do that threaded thing
    }
}
