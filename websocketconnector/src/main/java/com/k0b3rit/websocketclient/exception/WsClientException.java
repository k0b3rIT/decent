package com.k0b3rit.websocketclient.exception;

public class WsClientException extends RuntimeException {

    public WsClientException() {
    }

    public WsClientException(String message) {
        super(message);
    }

    public WsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public WsClientException(Throwable cause) {
        super(cause);
    }

    public WsClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
