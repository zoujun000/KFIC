package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.dto.DashboardStatsDTO;
import com.freight.dto.OrderSummaryDTO;
import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;
import com.freight.mapper.CustomerMapper;
import com.freight.mapper.FreightOrderMapper;
import com.freight.service.DashboardService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FreightOrderMapper orderMapper;
    private final CustomerMapper customerMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public DashboardStatsDTO getStats() {
        final Long currentUserId = SecurityUtil.isAdmin() ? null : SecurityUtil.getCurrentUserId();

        // 1. 客户总数
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0);
        if (currentUserId != null) customerWrapper.eq(Customer::getCreatedBy, currentUserId);
        long totalCustomers = customerMapper.selectCount(customerWrapper);

        // 2. 订单总数
        LambdaQueryWrapper<FreightOrder> totalWrapper = new LambdaQueryWrapper<FreightOrder>()
                .eq(FreightOrder::getDeleted, 0);
        if (currentUserId != null) totalWrapper.eq(FreightOrder::getCreatedBy, currentUserId);
        long totalOrders = orderMapper.selectCount(totalWrapper);

        // 3. 进行中（进仓/走船/已到港）
        LambdaQueryWrapper<FreightOrder> inProgressWrapper = new LambdaQueryWrapper<FreightOrder>()
                .eq(FreightOrder::getDeleted, 0)
                .in(FreightOrder::getStatus, "进仓", "走船", "已到港");
        if (currentUserId != null) inProgressWrapper.eq(FreightOrder::getCreatedBy, currentUserId);
        long inProgressOrders = orderMapper.selectCount(inProgressWrapper);

        // 4. 已完成（已提货）
        LambdaQueryWrapper<FreightOrder> completedWrapper = new LambdaQueryWrapper<FreightOrder>()
                .eq(FreightOrder::getDeleted, 0)
                .eq(FreightOrder::getStatus, "已提货");
        if (currentUserId != null) completedWrapper.eq(FreightOrder::getCreatedBy, currentUserId);
        long completedOrders = orderMapper.selectCount(completedWrapper);

        // 5. 最近 10 条订单
        LambdaQueryWrapper<FreightOrder> recentWrapper = new LambdaQueryWrapper<FreightOrder>()
                .eq(FreightOrder::getDeleted, 0)
                .orderByDesc(FreightOrder::getCreateTime)
                .last("LIMIT 10");
        if (currentUserId != null) recentWrapper.eq(FreightOrder::getCreatedBy, currentUserId);
        List<FreightOrder> recentOrders = orderMapper.selectList(recentWrapper);

        // 批量查客户名
        Set<Long> customerIds = recentOrders.stream()
                .map(FreightOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, String> customerNameMap = customerIds.isEmpty()
                ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Customer::getCompanyName, (a, b) -> a));

        List<OrderSummaryDTO> orderSummaries = recentOrders.stream().map(o -> {
            OrderSummaryDTO dto = new OrderSummaryDTO();
            dto.setId(o.getId());
            dto.setOrderSo(o.getOrderSo());
            dto.setOrderNo(o.getOrderNo());
            dto.setCustomerId(o.getCustomerId());
            dto.setCustomerName(customerNameMap.getOrDefault(o.getCustomerId(), "—"));
            dto.setShipType(o.getShipType());
            dto.setTradeTerms(o.getTradeTerms());
            dto.setOrigin(o.getOrigin());
            dto.setDestination(o.getDestination());
            dto.setCargoName(o.getCargoName());
            dto.setTotalAmount(o.getTotalAmount());
            dto.setStatus(o.getStatus());
            dto.setEtd(o.getEtd());
            dto.setEta(o.getEta());
            dto.setUpdateTime(o.getUpdateTime() != null ? o.getUpdateTime().format(DT_FMT) : null);
            return dto;
        }).toList();

        return DashboardStatsDTO.builder()
                .totalCustomers(totalCustomers)
                .totalOrders(totalOrders)
                .inProgressOrders(inProgressOrders)
                .completedOrders(completedOrders)
                .recentOrders(orderSummaries)
                .build();
    }
}
