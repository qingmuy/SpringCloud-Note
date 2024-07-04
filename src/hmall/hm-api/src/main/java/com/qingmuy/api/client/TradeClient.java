package com.qingmuy.api.client;

import com.qingmuy.api.domain.po.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("pay-service")
public interface TradeClient {
    @PostMapping("/orders/update")
    Long updateById(@RequestParam("order") Order order);
}
