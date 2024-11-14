package com.k0b3rit.marketdataprovider;

import com.google.common.base.Stopwatch;
import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.domain.utils.ThreadFactories;
import com.k0b3rit.domain.utils.log.LogColoringUtil;
import com.k0b3rit.marketdataprovider.exchange.ExchageClient;
import com.k0b3rit.marketdataprovider.exchange.ExchangeClientProvider;
import com.k0b3rit.marketdataprovider.model.MarketDataObserver;
import com.k0b3rit.websocketclient.exception.WsClientException;
import com.k0b3rit.websocketclient.model.WSClientMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class MarketDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataProvider.class);

    private static final long MARKET_DATA_PROCESSING_TIME_TRESHOLD = 50;

    @Autowired
    private ExchangeClientProvider exchangeClientProvider;

    @Autowired
    private MeterRegistry meterRegistry;

    private ConcurrentHashMap<MarketDataIdentifier, List<MarketDataObserver>> subscribers = new ConcurrentHashMap<>();

    private HashMap<MarketDataObserver, ExecutorService> observerExecutor = new HashMap<>();

    private Map<String, Counter> marketDataStatusCounterMap = new HashMap<>();
    private Map<String, Gauge> executorTaskGaugeMap = new HashMap<>();

    private Map<MarketDataObserver, Timer> observerResponseHistogram = new HashMap<>();
    private Map<ExchageClient, Counter> exchangeClientRestart = new HashMap<>();

    private ExecutorService providerExecutor = Executors.newFixedThreadPool(1, ThreadFactories.MARKET_DATA_PROVIDER_THREADFACTORY);

    public MarketDataProvider() {

    }

    public void subscribeToMarketData(MarketDataIdentifier marketDataIdentifier, MarketDataObserver observer) throws WsClientException {

        ExchageClient exchangeAdapter = exchangeClientProvider.getExchangeClient(marketDataIdentifier.getExchange());

        if (!subscribers.containsKey(marketDataIdentifier)) {
            subscribers.put(marketDataIdentifier, new ArrayList<>());
            initAndAddNewObserver(marketDataIdentifier, observer);
            exchangeAdapter.subscribe(marketDataIdentifier);
        } else {
            if (!subscribers.get(marketDataIdentifier).stream().filter(subscriber -> subscriber == observer).findFirst().isPresent()) {
                initAndAddNewObserver(marketDataIdentifier, observer);
            }
        }
    }

    public void dataInconsistencyDetected(MarketDataIdentifier marketDataIdentifier) {
        providerExecutor.submit(() -> {
            ExchageClient exchangeAdapter = exchangeClientProvider.getExchangeClient(marketDataIdentifier.getExchange());
            exchangeAdapter.restart();
        });
    }

    public void onExchangeAdapterRestart(Exchange restartedExchange) {
        exchangeClientRestart.computeIfAbsent(exchangeClientProvider.getExchangeClient(restartedExchange), k -> Counter.builder("exchange_client_restart")
                .tag("exchange", restartedExchange.toString())
                .register(meterRegistry)).increment(1);

        providerExecutor.submit(() -> {
            subscribers.forEachKey(1, (marketDataIdentifier) -> {
                try {
                    if (marketDataIdentifier.getExchange() == restartedExchange) {
                        ExchageClient exchangeAdapter = exchangeClientProvider.getExchangeClient(restartedExchange);
                        exchangeAdapter.subscribe(marketDataIdentifier);
                    }
                } catch (WsClientException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void initAndAddNewObserver(MarketDataIdentifier subscribeDefinition, MarketDataObserver observer) {
        initMetrics(observer);
        observerExecutor.put(observer, Executors.newFixedThreadPool(1, ThreadFactories.OBSERVER_PROCESSOR_THREADFACTORY)); //We have to use 1 thread per observer to preserve message orders
        subscribers.get(subscribeDefinition).add(observer);
        LOGGER.info(LogColoringUtil.inBlue("%s : %s", subscribeDefinition, observer.getClass().getSimpleName()));
    }

    private void initMetrics(MarketDataObserver marketDataObserver) {
        observerResponseHistogram.computeIfAbsent(marketDataObserver, k -> Timer.builder("market_data_process_time")
                .description("Time the observer takes to process the market data")
                .tag("observer", marketDataObserver.toString())
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99) // Publishes percentiles
                .maximumExpectedValue(Duration.ofMillis(50)) // Sets the maximum expected value to 1 second
                .register(meterRegistry));
    }

    private void increment(MarketDataIdentifier dataId) {
        Counter statusCounter = marketDataStatusCounterMap.computeIfAbsent(
                dataId.toString(), k -> Counter.builder("market_data")
                        .tag("data_id", dataId.toString())
                        .register(meterRegistry));
        statusCounter.increment(1);
    }

    public void measureQueueStatus(ExecutorService executorService, MarketDataObserver marketDataObserver) {

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;

        executorTaskGaugeMap.computeIfAbsent(marketDataObserver.toString(), k -> Gauge.builder("observer_executor_task_count", tpe, (ex) -> ex.getQueue().size())
                .tag("observer", marketDataObserver.toString())
                .register(meterRegistry));
        if (tpe.getQueue().size() > 1000) {
            LOGGER.info(String.format("ExecutorService [%s] status: Tasks in queue: [%s] Active tasks: [%s] All Task: [%s]", marketDataObserver, tpe.getQueue().size(), tpe.getActiveCount(), tpe.getTaskCount()));
        }
    }

    public void onMarketDataAvailable(MarketDataIdentifier marketDataIdentifier, WSClientMessage marketData) {
        increment(marketDataIdentifier);
        for (MarketDataObserver marketDataObserver : subscribers.get(marketDataIdentifier)) {

            ExecutorService executorService = observerExecutor.get(marketDataObserver);

            measureQueueStatus(executorService, marketDataObserver);

            executorService.execute(() -> {
                try {

                    Stopwatch stopwatch = Stopwatch.createStarted();

                    marketDataObserver.onMarketDataReceived(marketDataIdentifier, marketData);

                    stopwatch.stop();
                    long marketDataProcessTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                    observerResponseHistogram.get(marketDataObserver).record(marketDataProcessTime, TimeUnit.MILLISECONDS);
                    if (marketDataProcessTime > MARKET_DATA_PROCESSING_TIME_TRESHOLD) {
                        LOGGER.warn(LogColoringUtil.inYellow("MarketDataObserver [%s] took too long [%d]ms (max: [%d]ms) to handle received market data [%s]! Please move any heavy business logic to a different thread!", marketDataObserver, marketDataProcessTime, MARKET_DATA_PROCESSING_TIME_TRESHOLD, marketDataIdentifier.getMarketDataType()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void unsubscribeFromMarketData(MarketDataIdentifier marketDataIdentifier, MarketDataObserver marketDataObserver) {
        if (!subscribers.containsKey(marketDataIdentifier)) {
            return;
        }
        ExchageClient exchageClient = exchangeClientProvider.getExchangeClient(marketDataIdentifier.getExchange());
        exchageClient.unsubscribe(marketDataIdentifier);

        subscribers.get(marketDataIdentifier).remove(marketDataObserver);
        if (subscribers.get(marketDataIdentifier).isEmpty()) {
            subscribers.remove(marketDataIdentifier);
        }
    }
}