package com.freight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freight.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
