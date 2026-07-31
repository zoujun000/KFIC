package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public IPage<Customer> page(String keyword, Integer status, String customerType,
                                Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Customer::getCompanyName, keyword)
                        .or().like(Customer::getContactName, keyword)
                        .or().like(Customer::getPhone, keyword)
                        .or().like(Customer::getWechat, keyword)
                        .or().like(Customer::getWhatsapp, keyword)
                        .or().like(Customer::getEmail, keyword))
                .eq(status != null, Customer::getStatus, status)
                .eq(StringUtils.hasText(customerType), Customer::getCustomerType, customerType)
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
    public Map<String, Long> getStats() {
        LambdaQueryWrapper<Customer> baseWrapper = new LambdaQueryWrapper<>();
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) baseWrapper.eq(Customer::getCreatedBy, userId);
        }
        Long totalCount = customerMapper.selectCount(baseWrapper);

        LambdaQueryWrapper<Customer> activeWrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 1);
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) activeWrapper.eq(Customer::getCreatedBy, userId);
        }
        Long activeCount = customerMapper.selectCount(activeWrapper);

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("active", activeCount);
        return stats;
    }

    @Override
    public void save(Customer customer) {
        if (!StringUtils.hasText(customer.getCompanyName())) {
            throw new BusinessException("公司名称不能为空");
        }
        if (!StringUtils.hasText(customer.getCustomerCode())) {
            customer.setCustomerCode(generateCustomerCode());
        }
        customer.setCreatedBy(SecurityUtil.getCurrentUserId());
        // 记录了营业执照的上传人
        if (StringUtils.hasText(customer.getPhotoUrl())) {
            customer.setLicenseUploadedBy(SecurityUtil.getCurrentUserId());
        }
        // 防止前端传入非法字段
        // 统一 customerType 为小写
        if (StringUtils.hasText(customer.getCustomerType())) {
            customer.setCustomerType(customer.getCustomerType().toLowerCase());
        }
        customer.setId(null);
        customer.setDeleted(null);
        customer.setCreateTime(null);
        customer.setUpdateTime(null);
        customerMapper.insert(customer);
    }

    @Override
    public void update(Customer customer) {
        if (customer.getId() == null) throw new BusinessException("客户ID不能为空");

        // 带了营业执照就更新上传人为当前操作人（记录最后上传者）
        if (StringUtils.hasText(customer.getPhotoUrl())) {
            customer.setLicenseUploadedBy(SecurityUtil.getCurrentUserId());
        }

        // 统一 customerType 为小写
        if (StringUtils.hasText(customer.getCustomerType())) {
            customer.setCustomerType(customer.getCustomerType().toLowerCase());
        }

        // 【安全修复】清除不允许前端篡改的敏感字段
        customer.setCreatedBy(null);
        customer.setCustomerCode(null);
        customer.setDeleted(null);
        customer.setCreateTime(null);
        customer.setUpdateTime(null);

        LambdaUpdateWrapper<Customer> wrapper = new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, customer.getId());
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(Customer::getCreatedBy, SecurityUtil.getCurrentUserId());
        }
        int rows = customerMapper.update(customer, wrapper);
        if (rows == 0) throw new BusinessException("客户不存在或无权修改");
    }

    @Override
    public void delete(Long id) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getId, id);
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(Customer::getCreatedBy, SecurityUtil.getCurrentUserId());
        }
        int rows = customerMapper.delete(wrapper);
        if (rows == 0) throw new BusinessException("客户不存在或无权删除");
    }

    /**
     * 生成客户编号：CUS + 时间戳 + 4位随机数，避免并发冲突
     */
    private String generateCustomerCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "CUS" + timestamp + random;
    }
}
