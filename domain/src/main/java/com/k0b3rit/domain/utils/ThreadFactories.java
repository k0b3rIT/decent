package com.k0b3rit.domain.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactories {

    private static AtomicInteger counter = new AtomicInteger(0);

    public static ThreadFactory OBSERVER_PROCESSOR_THREADFACTORY = r -> new Thread(r, "Obserever-Processor_" + ThreadFactories.counter.addAndGet(1));
    public static ThreadFactory MARKET_DATA_PROVIDER_THREADFACTORY = r -> new Thread(r, "MarkedDataProvider_" + ThreadFactories.counter.addAndGet(1));


}
