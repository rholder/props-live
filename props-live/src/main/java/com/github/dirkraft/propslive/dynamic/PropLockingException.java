package com.github.dirkraft.propslive.dynamic;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class PropLockingException extends RuntimeException {
    public PropLockingException() {
    }

    public PropLockingException(String message) {
        super(message);
    }

    public PropLockingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropLockingException(Throwable cause) {
        super(cause);
    }

    public PropLockingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
