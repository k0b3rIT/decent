package com.k0b3rit.marketdataprovider.exchange.impl;

import com.alibaba.fastjson.JSON;
import com.k0b3rit.marketdataprovider.MarketDataProvider;
import com.k0b3rit.marketdataprovider.exchange.ExchageClient;
import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.domain.model.CustomPropertKey;
import com.k0b3rit.domain.model.MarketDataIdentifier;
import com.k0b3rit.domain.model.MarketDataType;
import com.k0b3rit.websocketclient.DecentClient;
import com.k0b3rit.websocketclient.exception.WsClientException;
import com.k0b3rit.websocketclient.model.WSClientMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Component
public class BybitExchange implements ExchageClient {

    private static final int MAX_SEND_ATTEMPT = 5;

    private DecentClient webSocketClient;

    private AtomicLong reqId = new AtomicLong(0);

    private Map<String, Consumer<WSClientMessage>> controlMessageConsumers = new ConcurrentHashMap<>();

    private static final String SERVER_URI = "wss://stream.bybit.com/v5/public/spot";

    private static final Logger LOGGER = LoggerFactory.getLogger(BybitExchange.class);

    private Map<String, MarketDataIdentifier> marketDataIdentifierCache = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private MarketDataProvider marketDataProvider;

    public BybitExchange(@Autowired MarketDataProvider marketDataProvider) {
        super();
        this.marketDataProvider = marketDataProvider;
        this.initWSClient();
        try {
            this.webSocketClient.connectBlocking();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initWSClient() {
        try {
            DecentClient webSocketClient = new DecentClient(new URI(SERVER_URI), this::messageProcessor, this::controlMessageProcessor, this::onClientClose, this::onClientOpen);
            webSocketClient.setConnectionLostTimeout(5);
            webSocketClient.setTcpNoDelay(true);
            this.webSocketClient = webSocketClient;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Exchange getExchange() {
        return Exchange.BYBIT;
    }

    public synchronized void restart() {
        try {
            this.webSocketClient.closeBlocking();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribe(MarketDataIdentifier marketDataIdentifier) {
        LOGGER.info("Subscribe [%s]".formatted(marketDataIdentifier));
        String topic;
        if (marketDataIdentifier.getMarketDataType() == MarketDataType.ORDER_BOOK) {
            topic = "orderbook.%s.%s".formatted(marketDataIdentifier.getPropertyByKey(CustomPropertKey.LEVEL), marketDataIdentifier.getPropertyByKey(CustomPropertKey.SYMBOL).toUpperCase());
        } else {
            throw new RuntimeException("Unsupported market data type [%s]".formatted(marketDataIdentifier.getMarketDataType()));
        }
        String reqId = String.valueOf(this.reqId.incrementAndGet());
        this.sendAndWaitForResponse(new BybitModel.OpMessage(reqId, BybitModel.OpMessageType.subscribe.name(), new String[]{topic}), reqId);
    }

    @Override
    public void unsubscribe(MarketDataIdentifier marketDataIdentifier) throws WsClientException {
        String topic;
        if (marketDataIdentifier.getMarketDataType() == MarketDataType.ORDER_BOOK) {
            topic = "orderbook.%s.%s".formatted(marketDataIdentifier.getPropertyByKey(CustomPropertKey.LEVEL), marketDataIdentifier.getPropertyByKey(CustomPropertKey.SYMBOL).toUpperCase());
        } else {
            throw new RuntimeException("Unsupported market data type [%s]".formatted(marketDataIdentifier.getMarketDataType()));
        }
        String reqId = String.valueOf(this.reqId.incrementAndGet());
        this.sendAndWaitForResponse(new BybitModel.OpMessage(reqId, BybitModel.OpMessageType.unsubscribe.name(), new String[]{topic}), reqId);
    }

    private void onClientClose(int code, String reason, boolean remote) {
        LOGGER.info("Connection closed with code [%d] and reason [%s]".formatted(code, reason));
        executorService.schedule(()->{
                initWSClient();
                try {
                    this.webSocketClient.connectBlocking();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }, 2, TimeUnit.SECONDS);

    }

    private void onClientOpen() {
        this.marketDataProvider.onExchangeAdapterRestart(Exchange.BYBIT);
    }

    private void sendAndWaitForResponse(BybitModel.WsMessage message, String reqId) throws WsClientException {
        AtomicBoolean success = new AtomicBoolean(false);

        controlMessageConsumers.put(reqId, (m) -> {
            JSONObject jsonMessage = m.getData();
            synchronized (success) {
                if (jsonMessage.getString("req_id").equals(reqId)) {
                    if (jsonMessage.getBoolean("success") == true) {
                        success.set(true);
                        success.notify();
                    } else {
                        success.set(false);
                        success.notify();
                    }
                }
            }
        });

        this.send(message);

        synchronized (success) {
            try {
                success.wait(5000);
            } catch (InterruptedException e) {
                throw new WsClientException(e);
            }
            controlMessageConsumers.remove(reqId);
            if (!success.get()) {
                throw new WsClientException("Failed to execute the operation [%s]".formatted(message));
            }
        }
        LOGGER.info("Operation [%s] executed successfully".formatted(message));
    }

    private void controlMessageProcessor(WSClientMessage wsClientMessage) {
        controlMessageConsumers.forEach((reqId, consumer) -> consumer.accept(wsClientMessage));
    }

    private void messageProcessor(WSClientMessage wsClientMessage) {
        JSONObject jsonObject = wsClientMessage.getData();
        String topic = jsonObject.getString("topic");
        marketDataProvider.onMarketDataAvailable(getMarketDataIdentifier(topic), wsClientMessage);
    }

    private MarketDataIdentifier getMarketDataIdentifier(String topic) {
        if (!marketDataIdentifierCache.containsKey(topic)) {
            String[] topicParts = topic.split("\\.");
            if (topicParts.length != 3) {
                throw new IllegalArgumentException("Invalid topic [%s]".formatted(topic));
            }
            String type = topicParts[0];
            String level = topicParts[1];
            String marketSymbol = topicParts[2];
            marketDataIdentifierCache.put(topic, new MarketDataIdentifier(Exchange.BYBIT, getMarketDataTypeFromTopicType(type)).addCustomProperty(CustomPropertKey.SYMBOL, marketSymbol).addCustomProperty(CustomPropertKey.LEVEL, level));
        }
        return marketDataIdentifierCache.get(topic);
    }

    private MarketDataType getMarketDataTypeFromTopicType(String topicType) {
        switch (topicType) {
            case "orderbook":
                return MarketDataType.ORDER_BOOK;
            default:
                throw new IllegalArgumentException("Unsupported topic type [%s]".formatted(topicType));
        }
    }

    private void send(BybitModel.WsMessage message) throws WsClientException {
        int attempts = 0;
        while (true) {
            Exception exception;
            attempts++;
            try {
                trySendMessage(message);
                return;
            } catch (Exception e) {
                exception = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (attempts >= MAX_SEND_ATTEMPT) {
                throw new WsClientException("Failed to send message after [%d] attempts".formatted(attempts), exception);
            }
        }
    }

    private void trySendMessage(BybitModel.WsMessage message) {
        String messageStr = JSON.toJSONString(message);
        this.webSocketClient.send(messageStr);
    }

}
