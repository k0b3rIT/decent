package com.k0b3rit.domain.orderbook;

import java.util.Date;
import java.util.List;

public class OrderBookDiffData {

    public final List<OrderBookEntry> bids;
    public final List<OrderBookEntry> asks;
    public final long eventTime;
    public final String symbol;
    public final long firstUpldateId;
    public final long finalUpdateId;
    public final String eventType;

    public OrderBookDiffData(long eventTime, String symbol, List<OrderBookEntry> bids, List<OrderBookEntry> asks, long firstUpldateId, long finalUpdateId, String eventType) {
        this.bids = bids;
        this.asks = asks;
        this.eventTime = eventTime;
        this.symbol = symbol;
        this.firstUpldateId = firstUpldateId;
        this.finalUpdateId = finalUpdateId;
        this.eventType = eventType;
    }

    public List<OrderBookEntry> getBids() {
        return bids;
    }

    public List<OrderBookEntry> getAsks() {
        return asks;
    }

    public long getEventTime() {
        return eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getEventType() {
        return eventType;
    }

    public long getFirstUpldateId() {
        return firstUpldateId;
    }

    public long getFinalUpdateId() {
        return finalUpdateId;
    }

    @Override
    public String toString() {
        return "OrderBookDiffData{" +
                "eventTime=" + eventTime + " " + new Date(eventTime) +
                '}';
    }
}
