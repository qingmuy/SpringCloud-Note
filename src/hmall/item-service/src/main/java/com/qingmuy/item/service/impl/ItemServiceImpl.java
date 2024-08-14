package com.qingmuy.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.qingmuy.item.domain.dto.ItemDTO;
import com.qingmuy.item.domain.dto.OrderDetailDTO;
import com.qingmuy.item.domain.po.Item;
import com.qingmuy.item.mapper.ItemMapper;
import com.qingmuy.item.service.IItemService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    /**
     * 扣减余额
     * @param items 商品合集
     */
    @Override
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.qingmuy.item.mapper.ItemMapper.updateStock";
        boolean r;
        try {
            r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }

    /**
     * 查询商品
     * @param ids 商品id集合
     * @return 订单信息
     */
    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }
}
