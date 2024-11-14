package com.k0b3rit.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class MarketDataIdentifier {

    private final MarketDataType marketDataType;
    private final Exchange exchange;
    private final Map<CustomPropertKey, String> customProperties = new TreeMap<CustomPropertKey, String>();

    private String customProsStr = "";

    public MarketDataIdentifier(Exchange exchange, MarketDataType marketDataType) {
        this.exchange = exchange;
        this.marketDataType = marketDataType;
    }

    public MarketDataType getMarketDataType() {
        return marketDataType;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public MarketDataIdentifier addCustomProperty(CustomPropertKey key, String value) {
        customProperties.put(key, value);
        customProsStr = customProsToStr();
        return this;
    }

    public String getPropertyByKey(CustomPropertKey key) {
        return Optional.ofNullable(customProperties.get(key)).orElseThrow(() -> new IllegalArgumentException("No such key: [%s]".formatted(key.name())));
    }

    private String customProsToStr() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<CustomPropertKey, String> entry : customProperties.entrySet()) {
            sb.append(entry.getKey().name()).append("=").append(entry.getValue().toLowerCase().trim()).append(";");
        }
        return sb.toString();
    }

    public String getIdentifierString() {
        return String.format("%s-%s-%s", getExchange(), getMarketDataType(), customProsStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketDataIdentifier that = (MarketDataIdentifier) o;
        return marketDataType == that.marketDataType &&
                exchange == that.exchange && customProsStr.equals(that.customProsStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(marketDataType, exchange, customProsStr);
    }

    @Override
    public String toString() {
        return getIdentifierString();
    }
}
