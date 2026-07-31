package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.entity.Customer;

import java.util.Map;

public interface CustomerService {
    IPage<Customer> page(String keyword, Integer status, String customerType, Integer pageNum, Integer pageSize);
    Customer getById(Long id);
    void save(Customer customer);
    void update(Customer customer);
    void delete(Long id);
    Map<String, Long> getStats();
}
