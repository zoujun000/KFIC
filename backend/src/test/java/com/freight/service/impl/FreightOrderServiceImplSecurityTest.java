package com.freight.service.impl;

import com.freight.common.exception.BusinessException;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;
import com.freight.mapper.CustomerMapper;
import com.freight.mapper.FreightOrderMapper;
import com.freight.service.AttachmentPathService;
import com.freight.util.SecurityUtil;
import com.freight.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightOrderServiceImplSecurityTest {

    @Mock
    private FreightOrderMapper orderMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private AttachmentPathService attachmentPathService;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private FreightOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FreightOrderServiceImpl(orderMapper, customerMapper, attachmentPathService, snowflakeIdGenerator);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void attachmentDirectoryRejectsAnotherUsersOrder() {
        FreightOrder order = new FreightOrder();
        order.setCreatedBy(2L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        SecurityUtil.setCurrentUserId(1L);

        assertThatThrownBy(() -> service.getAttachmentDir(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问该订单");

        verify(customerMapper, never()).selectByIdIncludeDeleted(order.getCustomerId());
    }

    @Test
    void orderOperationsRejectMissingUserIdForNonAdmin() {
        FreightOrder order = new FreightOrder();
        order.setCreatedBy(1L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未获取到当前用户身份");
        assertThatThrownBy(() -> service.page(new OrderQueryDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未获取到当前用户身份");
    }

    @Test
    void orderDtoDoesNotAcceptCreatedBy() {
        assertThat(Arrays.stream(FreightOrderDTO.class.getDeclaredFields())
                .map(Field::getName))
                .doesNotContain("createdBy");
    }

    @Test
    void createOrderRequiresExistingCustomer() {
        FreightOrderDTO dto = new FreightOrderDTO();
        dto.setCustomerId(99L);
        when(customerMapper.selectById(99L)).thenReturn(null);
        SecurityUtil.setCurrentUserId(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户不存在");
        verify(orderMapper, never()).insert(any(FreightOrder.class));
    }

    @Test
    void updateOrderAllowsMissingCustomer() {
        // 客户可能已被删除，历史订单仍需可编辑，因此修改订单不校验客户存在性
        FreightOrder existing = new FreightOrder();
        existing.setCreatedBy(1L);
        existing.setCustomerId(99L);
        existing.setOrderNo("NO-1");
        when(orderMapper.selectById(1L)).thenReturn(existing);
        when(customerMapper.selectById(99L)).thenReturn(null);
        when(orderMapper.update(any(FreightOrder.class), any())).thenReturn(1);
        SecurityUtil.setCurrentUserId(1L);

        FreightOrderDTO dto = new FreightOrderDTO();
        dto.setId(1L);
        dto.setCustomerId(99L);

        service.update(dto);

        verify(orderMapper).update(any(FreightOrder.class), any());
    }

    @Test
    void deletingOrderKeepsAttachmentsForLogicalDeletion() {
        FreightOrder order = new FreightOrder();
        order.setCreatedBy(1L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.deleteById(1L)).thenReturn(1);
        SecurityUtil.setCurrentUserId(1L);

        service.delete(1L);

        verify(orderMapper).deleteById(1L);
        verifyNoInteractions(customerMapper, attachmentPathService);
    }

    @Test
    void updatesOrdersThatHaveReachedEta() {
        when(orderMapper.update(isNull(), any())).thenReturn(2);

        assertThat(service.updateStatusesByEta(LocalDate.of(2026, 9, 7))).isEqualTo(2);

        verify(orderMapper).update(isNull(), any());
    }
}
