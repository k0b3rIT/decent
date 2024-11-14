package com.k0b3rit.websocketclient.model;

import org.json.JSONObject;


public class WSClientMessage {

    private final Type type;
    private final String error;
    private final Exception exception;
    private final JSONObject data;

    public WSClientMessage(Type type, JSONObject data) {
        this.type = type;
        this.data = data;
        this.error = null;
        this.exception = null;
    }
    public WSClientMessage(Type type, String error, Exception exception) {
        this.type = type;
        this.error = error;
        this.exception = exception;
        this.data = null;
    }

    public Type getType() {
        return type;
    }

    public String getError() {
        return error;
    }

    public Exception getException() {
        return exception;
    }

    public JSONObject getData() {
        return data;
    }

    public enum Type {
        MESSAGE,
        ERROR,
    }
}
