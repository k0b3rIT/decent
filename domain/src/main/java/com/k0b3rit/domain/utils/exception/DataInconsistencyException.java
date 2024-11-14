package com.k0b3rit.domain.utils.exception;

public class DataInconsistencyException extends Exception {

    public DataInconsistencyException(String message) {
        super(message);
    }

    public DataInconsistencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataInconsistencyException(Throwable cause) {
        super(cause);
    }

    public DataInconsistencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
