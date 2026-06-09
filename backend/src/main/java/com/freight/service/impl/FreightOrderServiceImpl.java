package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;
import com.freight.mapper.CustomerMapper;
import com.freight.mapper.FreightOrderMapper;
import com.freight.service.FreightOrderService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FreightOrderServiceImpl implements FreightOrderService {

    private final FreightOrderMapper orderMapper;
    private final CustomerMapper customerMapper;

    private static final String ATTACHMENT_ROOT = System.getProperty("user.home") + "/Desktop";

    @Override
    public IPage<FreightOrder> page(OrderQueryDTO query) {
        LambdaQueryWrapper<FreightOrder> wrapper = buildBaseQuery(query)
                .orderByDesc(FreightOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    private LambdaQueryWrapper<FreightOrder> buildBaseQuery(OrderQueryDTO query) {
        // 精确匹配时 trim 掉首尾空格
        String so = query.getOrderSo();
        if (StringUtils.hasText(so)) so = so.trim();
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .eq(StringUtils.hasText(so), FreightOrder::getOrderSo, so)
                .eq(query.getCustomerId() != null, FreightOrder::getCustomerId, query.getCustomerId())
                .eq(StringUtils.hasText(query.getShipType()), FreightOrder::getShipType, query.getShipType())
                .eq(StringUtils.hasText(query.getStatus()), FreightOrder::getStatus, query.getStatus())
                .ge(query.getEtdStart() != null, FreightOrder::getEtd, query.getEtdStart())
                .le(query.getEtdEnd() != null, FreightOrder::getEtd, query.getEtdEnd());

        // 数据隔离：非管理员只能看自己的订单
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) wrapper.eq(FreightOrder::getCreatedBy, userId);
        }
        return wrapper;
    }

    @Override
    public FreightOrder getById(Long id) {
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(order.getCreatedBy())) {
                throw new BusinessException("无权访问该订单");
            }
        }
        return order;
    }

    @Override
    public void create(FreightOrderDTO dto) {
        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order);
        order.setOrderNo("ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        order.setStatus("进仓");
        order.setCreatedBy(SecurityUtil.getCurrentUserId());
        orderMapper.insert(order);

        // 自动创建订单附件文件夹
        createOrderDir(order);
    }

    @Override
    public void update(FreightOrderDTO dto) {
        if (dto.getId() == null) throw new BusinessException("订单ID不能为空");
        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order);

        LambdaUpdateWrapper<FreightOrder> wrapper = new LambdaUpdateWrapper<FreightOrder>()
                .eq(FreightOrder::getId, dto.getId());
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, SecurityUtil.getCurrentUserId());
        }

        int rows = orderMapper.update(order, wrapper);
        if (rows == 0) throw new BusinessException("订单不存在或无权修改");
    }

    @Override
    public void updateStatus(Long id, String status) {
        LambdaUpdateWrapper<FreightOrder> wrapper = new LambdaUpdateWrapper<FreightOrder>()
                .eq(FreightOrder::getId, id)
                .set(FreightOrder::getStatus, status);
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, SecurityUtil.getCurrentUserId());
        }
        int rows = orderMapper.update(null, wrapper);
        if (rows == 0) throw new BusinessException("订单不存在或无权修改");
    }

    @Override
    public void delete(Long id) {
        // 先查出订单信息（删前需要订单数据来定位文件夹）
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(order.getCreatedBy())) {
                throw new BusinessException("无权删除该订单");
            }
        }

        // 先删文件
        deleteOrderDir(order);

        // 再删数据库记录
        int rows = orderMapper.deleteById(id);
        if (rows == 0) throw new BusinessException("删除失败");
    }

    @Override
    public java.util.List<FreightOrder> getEtaAlerts() {
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .isNotNull(FreightOrder::getEta)
                .apply("DATE_ADD(eta, INTERVAL 1 DAY) <= CURDATE()")
                .ne(FreightOrder::getStatus, "已提货")
                .orderByAsc(FreightOrder::getEta);

        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) wrapper.eq(FreightOrder::getCreatedBy, userId);
        }
        return orderMapper.selectList(wrapper);
    }

    // ==================== 文件系统管理 ====================

    /** 创建订单附件目录: ~/Desktop/{直客|同行}营业执照/{公司名}/{SO号}/ */
    private void createOrderDir(FreightOrder order) {
        try {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer == null) return;

            String licenseDir = "COLOAD".equalsIgnoreCase(customer.getCustomerType()) ? "同行营业执照" : "直客营业执照";
            String companyName = sanitize(customer.getCompanyName());
            String orderSo = sanitize(order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo());

            Path dir = Paths.get(ATTACHMENT_ROOT, licenseDir, companyName, orderSo);
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // 创建失败不阻断业务流程
        }
    }

    /** 删除订单附件目录（递归删除所有文件） */
    private void deleteOrderDir(FreightOrder order) {
        try {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer == null) return;

            String licenseDir = "COLOAD".equalsIgnoreCase(customer.getCustomerType()) ? "同行营业执照" : "直客营业执照";
            String companyName = sanitize(customer.getCompanyName());
            String orderSo = sanitize(order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo());

            Path dir = Paths.get(ATTACHMENT_ROOT, licenseDir, companyName, orderSo);
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
        } catch (IOException ignored) {
            // 删除失败不阻断业务流程
        }
    }

    /** 清理文件名中的非法字符 */
    private String sanitize(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
