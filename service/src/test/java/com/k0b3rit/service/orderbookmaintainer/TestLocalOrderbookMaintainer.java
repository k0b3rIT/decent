package com.k0b3rit.service.orderbookmaintainer;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

import static ir.cafebabe.math.utils.BigDecimalUtils.is;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestLocalOrderbookMaintainer {

    @Test
    public void testMaintainOrderbook() {
        LocalOrderbookMaintainer localOrderbookMaintainer = new LocalOrderbookMaintainer();
        TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
        asks.put(new BigDecimal("101.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("102.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("103.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("104.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("105.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("106.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("107.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("108.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("109.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("110.1"), new BigDecimal("10"));
        asks.put(new BigDecimal("111.1"), new BigDecimal("10"));

        TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>();
        bids.put(new BigDecimal("99.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("98.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("97.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("96.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("95.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("94.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("93.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("92.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("91.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("90.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("89.1"), new BigDecimal("10"));


        Pair<BigDecimal, BigDecimal> avgBuyCalcResult = localOrderbookMaintainer.calcAvgPriceOfTrade(asks.entrySet(), new BigDecimal("2000"), 8, 4);
        Pair<BigDecimal, BigDecimal> avgSellCalcResult = localOrderbookMaintainer.calcAvgPriceOfTrade(bids.descendingMap().entrySet(), new BigDecimal("2000"), 8, 4);
        BigDecimal bestBid = bids.lastKey();
        BigDecimal bestAsk = asks.firstKey();
        if (is(avgBuyCalcResult.getRight()).isNotZero() || is(avgSellCalcResult.getRight()).isNotZero()) {
            System.out.println("%s;%s;%s;%s - Orderbook depth was not sufficient leftovers: %s;%s".formatted(bestBid, bestAsk, avgBuyCalcResult.getLeft(), avgSellCalcResult.getLeft(), avgBuyCalcResult.getRight(), avgSellCalcResult.getRight()));
        } else {
            System.out.println("%s;%s;%s;%s".formatted(bestBid, bestAsk, avgBuyCalcResult.getLeft(), avgSellCalcResult.getLeft()));
        }

        assertTrue(is(bestBid).eq(new BigDecimal("99.1")));
        assertTrue(is(bestAsk).eq(new BigDecimal("101.1")));
        assertTrue(is(avgBuyCalcResult.getLeft()).eq(new BigDecimal("101.5920")));
        assertTrue(is(avgSellCalcResult.getLeft()).eq(new BigDecimal("98.5787")));
        assertTrue(is(avgBuyCalcResult.getRight()).isZero());
        assertTrue(is(avgSellCalcResult.getRight()).isZero());
    }

    @Test
    public void testMaintainOrderbook2() {
        LocalOrderbookMaintainer localOrderbookMaintainer = new LocalOrderbookMaintainer();
        TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.naturalOrder());
        asks.put(new BigDecimal("92444.1"), new BigDecimal("1.1"));
        asks.put(new BigDecimal("92444.2"), new BigDecimal("1.2"));
        asks.put(new BigDecimal("92444.3"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.4"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.5"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.6"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.7"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.8"), new BigDecimal("10"));
        asks.put(new BigDecimal("92444.9"), new BigDecimal("10"));

        TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>();
        bids.put(new BigDecimal("92443.1"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.2"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.3"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.4"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.5"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.6"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.7"), new BigDecimal("10"));
        bids.put(new BigDecimal("92443.8"), new BigDecimal("1.2"));
        bids.put(new BigDecimal("92443.9"), new BigDecimal("1.1"));


        Pair<BigDecimal, BigDecimal> avgBuyCalcResult = localOrderbookMaintainer.calcAvgPriceOfTrade(asks.entrySet(), new BigDecimal("250000"), 8, 4);
        Pair<BigDecimal, BigDecimal> avgSellCalcResult = localOrderbookMaintainer.calcAvgPriceOfTrade(bids.descendingMap().entrySet(), new BigDecimal("250000"), 8, 4);
        BigDecimal bestBid = bids.lastKey();
        BigDecimal bestAsk = asks.firstKey();
        if (is(avgBuyCalcResult.getRight()).isNotZero() || is(avgSellCalcResult.getRight()).isNotZero()) {
            System.out.println("%s;%s;%s;%s - Orderbook depth was not sufficient leftovers: %s;%s".formatted(bestBid, bestAsk, avgBuyCalcResult.getLeft(), avgSellCalcResult.getLeft(), avgBuyCalcResult.getRight(), avgSellCalcResult.getRight()));
        } else {
            System.out.println("%s;%s;%s;%s".formatted(bestBid, bestAsk, avgBuyCalcResult.getLeft(), avgSellCalcResult.getLeft()));
        }



        assertTrue(is(bestBid).eq(new BigDecimal("92443.9")));
        assertTrue(is(bestAsk).eq(new BigDecimal("92444.1")));
        assertTrue(is(avgBuyCalcResult.getLeft()).eq(new BigDecimal("92444.1743")));
        assertTrue(is(avgSellCalcResult.getLeft()).eq(new BigDecimal("92443.8257")));
        assertTrue(is(avgBuyCalcResult.getRight()).isZero());
        assertTrue(is(avgSellCalcResult.getRight()).isZero());

    }

}
