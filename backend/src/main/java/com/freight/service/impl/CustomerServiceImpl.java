package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.entity.Customer;
import com.freight.mapper.CustomerMapper;
import com.freight.service.CustomerService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public IPage<Customer> page(String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Customer::getCompanyName, keyword)
                        .or().like(Customer::getContactName, keyword)
                        .or().like(Customer::getPhone, keyword)
                        .or().like(Customer::getWechat, keyword)
                        .or().like(Customer::getWhatsapp, keyword))
                .orderByDesc(Customer::getCreateTime);

        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) wrapper.eq(Customer::getCreatedBy, userId);
        }

        return customerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(customer.getCreatedBy())) {
                throw new BusinessException("无权访问该客户");
            }
        }
        return customer;
    }

    @Override
    public void save(Customer customer) {
        if (!StringUtils.hasText(customer.getCustomerCode())) {
            customer.setCustomerCode("CUS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        customer.setCreatedBy(SecurityUtil.getCurrentUserId());
        customerMapper.insert(customer);
    }

    @Override
    public void update(Customer customer) {
        if (customer.getId() == null) throw new BusinessException("客户ID不能为空");
        Customer exist = customerMapper.selectById(customer.getId());
        if (exist == null) throw new BusinessException("客户不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(exist.getCreatedBy())) {
                throw new BusinessException("无权修改该客户");
            }
        }
        customerMapper.updateById(customer);
    }

    @Override
    public void delete(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(customer.getCreatedBy())) {
                throw new BusinessException("无权删除该客户");
            }
        }
        customerMapper.deleteById(id);
    }
}
