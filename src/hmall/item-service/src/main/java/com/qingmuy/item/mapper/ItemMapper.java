package com.qingmuy.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingmuy.item.domain.dto.OrderDetailDTO;
import com.qingmuy.item.domain.po.Item;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface ItemMapper extends BaseMapper<Item> {

    @Update("UPDATE item SET stock = stock - #{num} WHERE id = #{itemId}")
    void updateStock(OrderDetailDTO orderDetail);

    /**
     * 根据订单id恢复库存
     * @param orderId 订单id
     */
    @Update("update item t1 set stock = stock + (\n" +
            "    select num  from `hm-trade`.order_detail t2 where order_id = #{orderId} and t2.item_id = t1.id\n" +
            ") where exists (\n" +
            "    select 1 from `hm-trade`.order_detail t2 where order_id = #{orderId} and t2.item_id = t1.id\n" +
            ")")
    void restoreStock(Long orderId);
}
