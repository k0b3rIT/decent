package com.k0b3rit.domain.orderbook;

import java.math.BigDecimal;

public class OrderBookEntry {

    public final BigDecimal price;
    public final BigDecimal qty;

    public OrderBookEntry(BigDecimal price, BigDecimal qty) {
        this.price = price;
        this.qty = qty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQty() {
        return qty;
    }
}
