package com.qingmuy.trade.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.qingmuy.api.client.ItemClient;
import com.qingmuy.api.domain.dto.ItemDTO;
import com.qingmuy.api.domain.dto.OrderDetailDTO;
import com.qingmuy.api.domain.dto.OrderFormDTO;
import com.qingmuy.trade.constants.MqConstants;
import com.qingmuy.trade.domain.po.Order;
import com.qingmuy.trade.domain.po.OrderDetail;
import com.qingmuy.trade.mapper.OrderMapper;
import com.qingmuy.trade.service.IOrderDetailService;
import com.qingmuy.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final RabbitTemplate rabbitTemplate;

    private final IOrderDetailService detailService;

    private final ItemClient itemClient;

    @Resource
    OrderMapper orderMapper;



    /**
     * 创建订单
     * @param orderFormDTO 订单信息
     * @return 订单ID
     */
    @Override
    @Transactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        // 这里使用Set接值keyset，类型为class java.util.HashMap$KeySet
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        // cartClient.deleteCartItemByIds(itemIds);
        // 将购物车商品id与当前用户id封装在一起

        try {
            // 此处通过修改消息头部，将userId添加到消息头部，接收时，由于消息转换器被重写，所以userid会自动存入userContext
            rabbitTemplate.convertAndSend("trade.topic", "order.create", itemIds, message -> {
                message.getMessageProperties().setHeader("userId", UserContext.getUser());
                return message;
            });
        } catch (Exception e) {
            log.error("订单创建的消息发送失败", e);
        }

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 5.发送延迟消息，检测订单支付状态：延迟消息迟滞在交换机内
        rabbitTemplate.convertAndSend(
                MqConstants.DELAY_EXCHANGE_NAME,
                MqConstants.DELAY_ORDER_KEY,
                order.getId(),
                message -> {
                    message.getMessageProperties().setDelay(10000);
                    return message;
                }
        );
        return order.getId();
    }

    /**
     * 将订单标注为支付成功
     * @param orderId 订单id
     */
    @Override
    public void markOrderPaySuccess(Long orderId) {
        /*// 1.查询订单
        Order old = getById(orderId);
        // 2.判断订单状态
        if (old == null || old.getStatus() != 1) {
            // 订单不存在或者订单状态不是1，放弃处理
            return;
        }
        // 3.尝试更新订单
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);*/

        // 判断与更新是两步动作，因此在小概率下可能存在线程安全问题，因此改写如下
        lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
    }

    @Override
    public void cancelOrder(Long orderId) {
        // 将订单状态修改为已关闭
        Order order = orderMapper.selectById(orderId);

        if (order == null || order.getStatus() == 5) {
            return;
        }
        order.setStatus(5);
        orderMapper.updateById(order);
        // 恢复订单中已经扣除的库存
        /*LambdaQueryWrapper<OrderDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(qw);
        // 处理订单信息
        ArrayList<OrderDetailDTO> orderDetailDTOS = new ArrayList<>();
        for (OrderDetail od : orderDetails) {
            OrderDetailDTO orderDetailDTO = new OrderDetailDTO();
            BeanUtils.copyProperties(od, orderDetailDTO);

            orderDetailDTOS.add(orderDetailDTO);
        }
        // 同步通讯调用，恢复订单库存
        itemClient.restoreStock(orderDetailDTOS);*/
        itemClient.restoreStock(orderId);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
