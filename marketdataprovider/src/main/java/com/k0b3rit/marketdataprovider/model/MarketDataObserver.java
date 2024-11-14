package com.k0b3rit.marketdataprovider.model;

import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.websocketclient.model.WSClientMessage;

public interface MarketDataObserver {

    void onMarketDataReceived(MarketDataIdentifier marketDataIdentifier, WSClientMessage marketData);
}
