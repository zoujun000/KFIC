package com.freight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freight.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 按 ID 查客户，包含已逻辑删除的记录。
     * 用于订单附件等场景：客户被删除后，其历史订单仍需正常访问。
     */
    @Select("SELECT * FROM customer WHERE id = #{id}")
    Customer selectByIdIncludeDeleted(@Param("id") Long id);
}
