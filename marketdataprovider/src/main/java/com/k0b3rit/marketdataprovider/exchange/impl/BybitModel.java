package com.k0b3rit.marketdataprovider.exchange.impl;

import java.util.Arrays;

public class BybitModel {

    public interface WsMessage {
    }

    public static class OpResponse implements WsMessage {
        public OpResponse(boolean success, String ret_msg, String conn_id, String op) {
            this.success = success;
            this.ret_msg = ret_msg;
            this.conn_id = conn_id;
            this.op = op;
        }

        public boolean success;
        public String ret_msg;
        public String conn_id;
        public String op;
    }

    public static class OpMessage implements WsMessage {

        public OpMessage(String req_id, String op, String[] args) {
            this.req_id = req_id;
            this.op = op;
            this.args = args;
        }

        public String req_id;
        public String op;
        public String[] args;

        @Override
        public String toString() {
            return "OpMessage{" +
                    "req_id='" + req_id + '\'' +
                    ", op='" + op + '\'' +
                    ", args=" + Arrays.toString(args) +
                    '}';
        }
    }

    public enum OpMessageType {
        subscribe,
        unsubscribe
    }

    public static class Orderbook {

        public String topic;
        public String type;
        public long ts;

        public Data data;
        public long cts;

        public class Data{
            public String s;
            public String[][] b;
            public String[][] a;
            public int u;
            public long seq;
        }
    }
}
