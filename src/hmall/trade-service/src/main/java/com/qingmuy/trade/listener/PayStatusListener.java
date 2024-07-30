package com.qingmuy.trade.listener;

import com.qingmuy.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayStatusListener {

    private final IOrderService orderService;

    /**
     * 监听消息队列，处理订单状态
     * @param orderId 订单id
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    private void listenPaySuccess(Long orderId) {
        log.info("监听到队列消息：{}", orderId);
        orderService.markOrderPaySuccess(orderId);
    }
}
