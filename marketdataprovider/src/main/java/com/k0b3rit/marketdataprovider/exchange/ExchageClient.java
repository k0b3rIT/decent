package com.k0b3rit.marketdataprovider.exchange;

import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.websocketclient.exception.WsClientException;

public interface ExchageClient {

    Exchange getExchange();

    void subscribe(MarketDataIdentifier marketDataIdentifier) throws WsClientException;

    void unsubscribe(MarketDataIdentifier marketDataIdentifier) throws WsClientException;

    void restart();
}
