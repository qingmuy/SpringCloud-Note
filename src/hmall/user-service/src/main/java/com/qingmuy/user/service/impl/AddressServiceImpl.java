package com.qingmuy.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingmuy.user.domain.po.Address;
import com.qingmuy.user.mapper.AddressMapper;
import com.qingmuy.user.service.IAddressService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

}
