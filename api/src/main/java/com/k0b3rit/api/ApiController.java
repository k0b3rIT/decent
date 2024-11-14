package com.k0b3rit.api;

import com.k0b3rit.domain.model.Exchange;
import com.k0b3rit.domain.model.Level;
import com.k0b3rit.service.orderbookmaintainer.OrderbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orderbook")
public class ApiController {

    @Autowired
    private OrderbookService orderbookService;

    @PostMapping("/")
    public void maintainLocalOrderbook(@RequestParam Exchange exchange, @RequestParam String symbol, @RequestParam Level level) {
        orderbookService.maintainOrderbook(exchange, symbol, level.getLevel());
    }

    @DeleteMapping("/")
    public void stopMaintainLocalOrderbook(@RequestParam Exchange exchange, @RequestParam String symbol, @RequestParam Level level) {
        orderbookService.stopMaintainOrderbook(exchange, symbol, level.getLevel());
    }
}
