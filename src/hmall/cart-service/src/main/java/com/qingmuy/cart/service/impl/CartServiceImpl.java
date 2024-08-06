package com.qingmuy.cart.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.qingmuy.api.client.ItemClient;
import com.qingmuy.api.domain.dto.ItemDTO;
import com.qingmuy.cart.config.CartProperties;
import com.qingmuy.cart.domain.dto.CartFormDTO;
import com.qingmuy.cart.domain.po.Cart;
import com.qingmuy.cart.domain.vo.CartVO;
import com.qingmuy.cart.mapper.CartMapper;
import com.qingmuy.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单详情表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    private final ItemClient itemClient;

    private final CartProperties cartProperties;

    /**
     * 购物车增添商品
     * @param cartFormDTO 商品信息
     */
    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();

        // 2.判断是否已经存在
        if(checkItemExists(cartFormDTO.getItemId(), userId)){
            // 2.1.存在，则更新数量
            baseMapper.updateNum(cartFormDTO.getItemId(), userId);
            return;
        }
        // 2.2.不存在，判断是否超过购物车数量
        checkCartsFull(userId);

        // 3.新增购物车条目
        // 3.1.转换PO
        Cart cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
        // 3.2.保存当前用户
        cart.setUserId(userId);
        // 3.3.保存到数据库
        save(cart);
    }

    /**
     * 查询购物车内容
     * @return 购物车内商品列表
     */
    @Override
    public List<CartVO> queryMyCarts() {
        // 1.查询我的购物车列表
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, UserContext.getUser()).list();
        if (CollUtils.isEmpty(carts)) {
            return CollUtils.emptyList();
        }

        // 2.转换VO
        List<CartVO> vos = BeanUtils.copyList(carts, CartVO.class);

        // 3.处理VO中的商品信息
        handleCartItems(vos);

        // 4.返回
        return vos;
    }

    /**
     *
     * @param vos
     */
    private void handleCartItems(List<CartVO> vos) {
        // 1.获取商品id
        Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());

        // 2.查询商品
        /*// 发现item-service服务的实例列表
        List<ServiceInstance> instances = discoveryClient.getInstances("item-service");
        // 没有可用实例直接结束
        if (CollUtil.isEmpty(instances)) {
            return;
        }

        // 负载均衡，挑选实例：随机选择
        ServiceInstance instance = instances.get(RandomUtil.randomInt(instances.size()));
        ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
                instance.getUri() + "/items?ids={ids}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ItemDTO>>() {
                },
                Map.of("ids", CollUtil.join(itemIds, ","))  // 使用CollUtil将Set集合转为字符串
        );

        // 解析响应，获取数据
        if (!response.getStatusCode().is2xxSuccessful()) {
            // 查询失败，结束
            return;
        }

        List<ItemDTO> items = response.getBody();*/

        // 使用openfeign直接查询数据
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);

        if (CollUtil.isEmpty(items)) {
            // 查询数据为空，即查询失败
            return;
        }

        // 3.转为 id 到 item的map
        Map<Long, ItemDTO> itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, Function.identity()));
        // 4.写入vo
        for (CartVO v : vos) {
            ItemDTO item = itemMap.get(v.getItemId());
            if (item == null) {
                continue;
            }
            v.setNewPrice(item.getPrice());
            v.setStatus(item.getStatus());
            v.setStock(item.getStock());
        }
    }

    /**
     * 批量删除购物车内商品
     * @param itemIds 商品ids
     */
    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.getUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }

    /**
     * 检查用户购物车是否已经超出阈值
     * @param userId 用户id
     */
    private void checkCartsFull(Long userId) {
        Long count = lambdaQuery().eq(Cart::getUserId, userId).count();
        if (count >= cartProperties.getMaxItems()) {
            throw new BizIllegalException(StrUtil.format("用户购物车课程不能超过{}", cartProperties.getMaxItems()));
        }
    }

    /**
     * 检查商品是否存在于用户的购物车
     * @param itemId 商品id
     * @param userId 用户id
     * @return 结果：是否存在
     */
    private boolean checkItemExists(Long itemId, Long userId) {
        Long count = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }

    /**
     * 给RabbitMQ使用的删除购物车内商品方法
     * @param itemIds 商品信息列表
     * @param UserId 用户id
     */
    @Override
    public void removeCartByItemIds(Set<Long> itemIds, Long UserId) {
        // 1.构建删除条件，userId和itemId
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, UserId)
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(qw);
    }
}
