package com.k0b3rit.domain.orderbook;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.TreeMap;

public class LocalOrderBook {

    // MAP<Price, Quantity>
    private TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>();
    private TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private long lastUpdateId;

    public TreeMap<BigDecimal, BigDecimal> getBids() {
        return bids;
    }

    public void setBids(TreeMap<BigDecimal, BigDecimal> bids) {
        this.bids = bids;
    }

    public TreeMap<BigDecimal, BigDecimal> getAsks() {
        return asks;
    }

    public void setAsks(TreeMap<BigDecimal, BigDecimal> asks) {
        this.asks = asks;
    }

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalOrderBook that = (LocalOrderBook) o;
        return lastUpdateId == that.lastUpdateId &&
                Objects.equals(bids, that.bids) &&
                Objects.equals(asks, that.asks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bids, asks, lastUpdateId);
    }
}
