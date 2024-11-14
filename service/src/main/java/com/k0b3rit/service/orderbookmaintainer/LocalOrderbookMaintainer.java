package com.k0b3rit.service.orderbookmaintainer;

import com.alibaba.fastjson.JSON;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.domain.orderbook.LocalOrderBook;
import com.k0b3rit.domain.utils.exception.DataInconsistencyException;
import com.k0b3rit.marketdataprovider.MarketDataProvider;
import com.k0b3rit.marketdataprovider.exchange.impl.BybitModel;
import com.k0b3rit.marketdataprovider.model.MarketDataObserver;
import com.k0b3rit.websocketclient.model.WSClientMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalOrderbookMaintainer implements MarketDataObserver {

    @Autowired
    private MarketDataProvider marketDataProvider;

    private Map<String, LocalOrderBook> orderBooks = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalOrderbookMaintainer.class);

    private int i = 0;

    public void maintainOrderbook(MarketDataIdentifier marketDataIdentifier) {
        marketDataProvider.subscribeToMarketData(marketDataIdentifier, this);
    }

    public void stopMaintainOrderbook(MarketDataIdentifier marketDataIdentifier) {
        marketDataProvider.unsubscribeFromMarketData(marketDataIdentifier, this);
    }

    @Override
    public void onMarketDataReceived(MarketDataIdentifier marketDataIdentifier, WSClientMessage marketData) {
        i++;
        try {
            BybitModel.Orderbook ob = JSON.parseObject(marketData.getData().toString(), BybitModel.Orderbook.class);
            if (ob.type.equals("snapshot")) {
                handleSnapshot(ob, marketDataIdentifier);
            } else {
                LocalOrderBook localOrderBook = orderBooks.get(marketDataIdentifier.getIdentifierString());
                if (localOrderBook == null) {
                    LOGGER.error("No snapshot received before delta [%s]".formatted(ob.topic));
                    return;
                }
                handleDelta(ob, localOrderBook);
            }
            LocalOrderBook localOrderBook = orderBooks.get(marketDataIdentifier.getIdentifierString());
            BigDecimal avgBuy = calcAvgPriceOfTrade(localOrderBook.getAsks().entrySet(), 250_000);
            BigDecimal avgSell = calcAvgPriceOfTrade(localOrderBook.getBids().descendingMap().entrySet(), 250_000);
            BigDecimal bestBid = localOrderBook.getBids().lastKey();
            BigDecimal bestAsk = localOrderBook.getAsks().firstKey();
            System.out.println("%s;%s;%s;%s;%s".formatted(ob.cts, bestBid, bestAsk, avgBuy, avgSell));

            // Simulate data inconsistency
//            if (i % 2000 == 0) {
//                throw new DataInconsistencyException("Dummy exception to demonstrate the robustness of the system against data inconsistency.");
//            }

        } catch (DataInconsistencyException dataInconsistencyException) {
            LOGGER.error(dataInconsistencyException.getMessage());
            marketDataProvider.dataInconsistencyDetected(marketDataIdentifier);
        }

    }

    private BigDecimal calcAvgPriceOfTrade(Set<Map.Entry<BigDecimal, BigDecimal>> entrySet, double volume) {
        BigDecimal sum = new BigDecimal(0);
        BigDecimal allQuantity = new BigDecimal(0);
        for (Map.Entry<BigDecimal, BigDecimal> entry : entrySet) {
            BigDecimal price = entry.getKey();
            BigDecimal quantity = entry.getValue();
            double exc = price.doubleValue() * quantity.doubleValue();
            if (exc <= volume) {
                sum = sum.add(price.multiply(quantity));
                allQuantity = allQuantity.add(quantity);
                volume -= exc;
            } else {
                sum = sum.add(price.multiply(new BigDecimal(volume / exc)));
                allQuantity = allQuantity.add(new BigDecimal(volume / exc));
                break;
            }
            if (volume == 0) {
                break;
            }
        }
        return sum.divide(allQuantity, 3, BigDecimal.ROUND_HALF_UP);
    }

    private void handleSnapshot(BybitModel.Orderbook ob, MarketDataIdentifier marketDataIdentifier) {
        LOGGER.info("Snapshot received: " + marketDataIdentifier + " " + ob);
        TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
        for (String[] ask : ob.data.a) {
            asks.put(new BigDecimal(ask[0]), new BigDecimal(ask[1]));
        }
        TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>();
        for (String[] bid : ob.data.b) {
            bids.put(new BigDecimal(bid[0]), new BigDecimal(bid[1]));
        }
        LocalOrderBook localOrderBook = new LocalOrderBook();
        localOrderBook.setAsks(asks);
        localOrderBook.setBids(bids);
        localOrderBook.setLastUpdateId(ob.data.u);
        orderBooks.put(marketDataIdentifier.getIdentifierString(), localOrderBook);
    }

    private void handleDelta(BybitModel.Orderbook ob, LocalOrderBook localOrderBook) throws DataInconsistencyException {
        if (ob.data.u != localOrderBook.getLastUpdateId() + 1) {
            throw new DataInconsistencyException("Out of order update received [%s]".formatted(ob.topic));
        }
        if (ob.data.u <= localOrderBook.getLastUpdateId()) {
            LOGGER.info("Old update received: [%s]".formatted(ob.topic));
            return;
        }
        for (String[] ask : ob.data.a) {
            if (ask[1].equals("0")) {
                localOrderBook.getAsks().remove(new BigDecimal(ask[0]));
            } else {
                localOrderBook.getAsks().put(new BigDecimal(ask[0]), new BigDecimal(ask[1]));
            }
        }
        for (String[] bid : ob.data.b) {
            if (bid[1].equals("0")) {
                localOrderBook.getBids().remove(new BigDecimal(bid[0]));
            } else {
                localOrderBook.getBids().put(new BigDecimal(bid[0]), new BigDecimal(bid[1]));
            }
        }
        localOrderBook.setLastUpdateId(ob.data.u);
    }
}
