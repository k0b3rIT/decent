package com.k0b3rit.service.orderbookmaintainer;

import com.k0b3rit.domain.model.CustomPropertKey;
import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.domain.model.MarketDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderbookService {

    @Autowired
    private LocalOrderbookMaintainer localOrderbookMaintainer;

    public void maintainOrderbook(Exchange exchange, String symbol, String level) {
        MarketDataIdentifier marketDataIdentifier = new MarketDataIdentifier(exchange, MarketDataType.ORDER_BOOK).addCustomProperty(CustomPropertKey.SYMBOL, symbol.toUpperCase()).addCustomProperty(CustomPropertKey.LEVEL, level);
        localOrderbookMaintainer.maintainOrderbook(marketDataIdentifier);
    }

    public void stopMaintainOrderbook(Exchange exchange, String symbol, String level) {
        MarketDataIdentifier marketDataIdentifier = new MarketDataIdentifier(exchange, MarketDataType.ORDER_BOOK).addCustomProperty(CustomPropertKey.SYMBOL, symbol.toUpperCase()).addCustomProperty(CustomPropertKey.LEVEL, level);
        localOrderbookMaintainer.stopMaintainOrderbook(marketDataIdentifier);
    }
}
