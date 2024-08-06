package com.qingmuy.cart.listener;

import com.qingmuy.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderListener {

    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(name = "trade.topic", type = "topic"),
            key = "order.create"
    ))
    public void listenOrder(Map msg) {
        cartService.removeCartByItemIds((Set<Long>) msg.get("itemIds"), (Long) msg.get("userId"));
    }

}
