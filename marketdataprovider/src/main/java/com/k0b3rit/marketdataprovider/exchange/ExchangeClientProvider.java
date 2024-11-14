package com.k0b3rit.marketdataprovider.exchange;

import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.marketdataprovider.exchange.impl.BybitExchange;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ExchangeClientProvider implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Map<Exchange, Class<? extends ExchageClient>> exhangeClassMapping = new HashMap<>();

    {
        exhangeClassMapping.put(Exchange.BYBIT, BybitExchange.class);
    }

    public ExchageClient getExchangeClient(Exchange exchange) {
        return applicationContext.getBean(Optional.ofNullable(exhangeClassMapping.get(exchange)).orElseThrow(()->new IllegalArgumentException("Mapping for exchange [%] is not defined!".formatted(exchange))));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
