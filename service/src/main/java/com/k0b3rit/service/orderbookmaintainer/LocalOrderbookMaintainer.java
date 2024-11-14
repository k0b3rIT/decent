package com.k0b3rit.service.orderbookmaintainer;

import com.alibaba.fastjson.JSON;
import com.google.common.annotations.VisibleForTesting;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.domain.orderbook.LocalOrderBook;
import com.k0b3rit.domain.utils.exception.DataInconsistencyException;
import com.k0b3rit.marketdataprovider.MarketDataProvider;
import com.k0b3rit.marketdataprovider.exchange.impl.BybitModel;
import com.k0b3rit.marketdataprovider.model.MarketDataObserver;
import com.k0b3rit.websocketclient.model.WSClientMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static ir.cafebabe.math.utils.BigDecimalUtils.*;

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
        assert Thread.currentThread().getName().equals("Obserever-Processor_1"); //Demonstrating the thread safety and the Websocket thread pool offloading
        i++;
        LocalOrderBook localOrderBook = orderBooks.get(marketDataIdentifier.getIdentifierString());
        try {
            BybitModel.Orderbook ob = JSON.parseObject(marketData.getData().toString(), BybitModel.Orderbook.class);
            if (ob.type.equals("snapshot")) {
                LocalOrderBook newOrderbook = handleSnapshot(ob, marketDataIdentifier);
                orderBooks.put(marketDataIdentifier.getIdentifierString(), newOrderbook);
                localOrderBook = newOrderbook;
            } else {
                if (localOrderBook == null) {
                    throw new DataInconsistencyException("No snapshot received before delta [%s]".formatted(ob.topic));
                }
                handleDelta(ob, localOrderBook);
            }

            calcAndPrintMarketImpact(localOrderBook);

//            Simulate data inconsistency
//            if (i % 500 == 0) {
//                throw new DataInconsistencyException("Dummy exception to demonstrate the robustness of the system against data inconsistency.");
//            }

        } catch (DataInconsistencyException dataInconsistencyException) {
            LOGGER.error(dataInconsistencyException.getMessage());
            marketDataProvider.dataInconsistencyDetected(marketDataIdentifier);
        }
    }

    private void calcAndPrintMarketImpact(LocalOrderBook localOrderBook) {
        int baseMinAccuracy = 8; // THE task asked for BTC, which require 8 min accuracy, and I did not want to spent more time to query the actual accuracy of each coin.
        int quoteMinAccuracy = 4; // THE task asked for USDT, which require 4 min accuracy.
        Pair<BigDecimal, BigDecimal> avgBuyCalcRes = calcAvgPriceOfTrade(localOrderBook.getAsks().entrySet(), new BigDecimal("250000"), baseMinAccuracy, quoteMinAccuracy);
        Pair<BigDecimal, BigDecimal> avgSellCalcRes = calcAvgPriceOfTrade(localOrderBook.getBids().descendingMap().entrySet(), new BigDecimal("250000"), baseMinAccuracy, quoteMinAccuracy);
        BigDecimal bestBid = localOrderBook.getBids().lastKey();
        BigDecimal bestAsk = localOrderBook.getAsks().firstKey();
        if (is(avgBuyCalcRes.getRight()).isNotZero() || is(avgSellCalcRes.getRight()).isNotZero()) {
            System.out.println("%s;%s;%s;%s - Orderbook depth was not sufficient. Leftovers: %s;%s".formatted(bestBid, bestAsk, avgBuyCalcRes.getLeft(), avgSellCalcRes.getLeft(), avgBuyCalcRes.getRight(), avgSellCalcRes.getRight()));
        } else {
            System.out.println("%s;%s;%s;%s".formatted(bestBid, bestAsk, avgBuyCalcRes.getLeft(), avgSellCalcRes.getLeft()));
        }
    }

    @VisibleForTesting
    Pair<BigDecimal,BigDecimal> calcAvgPriceOfTrade(Set<Map.Entry<BigDecimal, BigDecimal>> entrySet, BigDecimal initialBudget, int baseMinAccuracy, int quoteMinAccurancy) {
        BigDecimal remainingBudget = new BigDecimal(initialBudget.toString());
        BigDecimal allSpending = new BigDecimal(0);
        BigDecimal allQuantity = new BigDecimal(0);
        int divScale = (int) Math.round(Math.max(baseMinAccuracy, quoteMinAccurancy) * 1.5);
        for (Map.Entry<BigDecimal, BigDecimal> entry : entrySet) {
            BigDecimal price = entry.getKey();
            BigDecimal availableQuantityAtPriceLevel = entry.getValue();
            BigDecimal cost = price.multiply(availableQuantityAtPriceLevel);
            if (is(remainingBudget).gte(cost)) {
                allSpending = allSpending.add(price.multiply(availableQuantityAtPriceLevel));
                allQuantity = allQuantity.add(availableQuantityAtPriceLevel);
                remainingBudget = remainingBudget.subtract(cost);
            } else {
                BigDecimal p = remainingBudget.divide(cost, divScale, RoundingMode.FLOOR);
                BigDecimal adjustedQuantity = availableQuantityAtPriceLevel.multiply(p);
                BigDecimal adjustedCost = price.multiply(adjustedQuantity);
                allSpending = allSpending.add(adjustedCost);
                allQuantity = allQuantity.add(adjustedQuantity);
                remainingBudget = remainingBudget.subtract(adjustedCost);
            }
            if (is(remainingBudget.setScale(quoteMinAccurancy, RoundingMode.FLOOR)).isZero()) {
                break;
            }
        }
        BigDecimal leftover = new BigDecimal("0");
        if (is(remainingBudget.setScale(quoteMinAccurancy, RoundingMode.FLOOR)).isNotZero()) {
            //The orderbook was not deep enough to fulfill the requested volume
            leftover = initialBudget.subtract(allSpending);
//            for (Map.Entry<BigDecimal, BigDecimal> entry : entrySet) {
//                System.out.println(entry.getKey() + " " + entry.getValue());
//            }
        }

        return Pair.of(allSpending.divide(allQuantity, quoteMinAccurancy, BigDecimal.ROUND_HALF_UP), leftover);
    }

    private LocalOrderBook handleSnapshot(BybitModel.Orderbook ob, MarketDataIdentifier marketDataIdentifier) {
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
        return localOrderBook;
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
