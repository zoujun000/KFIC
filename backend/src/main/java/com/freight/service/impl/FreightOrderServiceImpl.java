package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;
import com.freight.mapper.FreightOrderMapper;
import com.freight.service.FreightOrderService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FreightOrderServiceImpl implements FreightOrderService {

    private final FreightOrderMapper orderMapper;

    @Override
    public IPage<FreightOrder> page(OrderQueryDTO query) {
        LambdaQueryWrapper<FreightOrder> wrapper = buildBaseQuery(query)
                .orderByDesc(FreightOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    private LambdaQueryWrapper<FreightOrder> buildBaseQuery(OrderQueryDTO query) {
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .like(StringUtils.hasText(query.getOrderSo()), FreightOrder::getOrderSo, query.getOrderSo())
                .eq(query.getCustomerId() != null, FreightOrder::getCustomerId, query.getCustomerId())
                .eq(StringUtils.hasText(query.getShipType()), FreightOrder::getShipType, query.getShipType())
                .eq(StringUtils.hasText(query.getStatus()), FreightOrder::getStatus, query.getStatus());

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
    }

    @Override
    public void update(FreightOrderDTO dto) {
        if (dto.getId() == null) throw new BusinessException("订单ID不能为空");
        FreightOrder exist = orderMapper.selectById(dto.getId());
        if (exist == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(exist.getCreatedBy())) {
                throw new BusinessException("无权修改该订单");
            }
        }
        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order);
        orderMapper.updateById(order);
    }

    @Override
    public void updateStatus(Long id, String status) {
        FreightOrder order = getById(id);
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        orderMapper.deleteById(id);
    }
}
