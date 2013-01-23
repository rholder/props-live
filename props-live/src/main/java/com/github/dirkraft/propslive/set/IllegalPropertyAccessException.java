package com.github.dirkraft.propslive.set;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class IllegalPropertyAccessException extends RuntimeException {
    public IllegalPropertyAccessException() {
    }

    public IllegalPropertyAccessException(String message) {
        super(message);
    }

    public IllegalPropertyAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPropertyAccessException(Throwable cause) {
        super(cause);
    }

    public IllegalPropertyAccessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
