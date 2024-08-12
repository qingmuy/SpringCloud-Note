package com.qingmuy.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingmuy.item.domain.dto.OrderDetailDTO;
import com.qingmuy.item.domain.po.Item;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
     * 遍历列表中的OrderDetailDTO对象，恢复库存
     * @param orderDetailDTOs 存储OrderDetailDTO的列表
     */
    void restoreStock(List<OrderDetailDTO> orderDetailDTOs);
}
