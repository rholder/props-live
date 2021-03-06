package com.github.dirkraft.propslive.dynamic.listen;

import org.apache.commons.lang3.builder.*;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropChange<VALUE> {

    private final VALUE old;
    private final VALUE now;

    public PropChange(VALUE old, VALUE now) {
        this.old = old;
        this.now = now;
    }

    public VALUE old() {
        return old;
    }

    public VALUE now() {
        return now;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("old", old).
                append("now", now).
                toString();
    }
}
