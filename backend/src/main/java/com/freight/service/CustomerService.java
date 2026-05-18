package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.entity.Customer;

public interface CustomerService {
    IPage<Customer> page(String keyword, Integer pageNum, Integer pageSize);
    Customer getById(Long id);
    void save(Customer customer);
    void update(Customer customer);
    void delete(Long id);
}
