package com.qingmuy.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qingmuy.pay.domain.dto.PayApplyDTO;
import com.qingmuy.pay.domain.dto.PayOrderFormDTO;
import com.qingmuy.pay.domain.po.PayOrder;

/**
 * <p>
 * 支付订单 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-16
 */
public interface IPayOrderService extends IService<PayOrder> {

    String applyPayOrder(PayApplyDTO applyDTO);

    void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO);
}
