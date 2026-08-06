package com.freight.service.impl;

import com.freight.common.exception.BusinessException;
import com.freight.entity.Customer;
import com.freight.mapper.CustomerMapper;
import com.freight.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplSecurityTest {

    @Mock
    private CustomerMapper customerMapper;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(customerMapper);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAdminCustomerOperationsRejectMissingUserId() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("测试客户");
        assertMissingUserId(() -> service.page(null, null, null, 1, 10));
        assertMissingUserId(() -> service.getById(1L));
        assertMissingUserId(service::getStats);
        assertMissingUserId(() -> service.save(customer));
        assertMissingUserId(() -> service.update(customer));
        assertMissingUserId(() -> service.delete(1L));

        verifyNoInteractions(customerMapper);
    }

    private void assertMissingUserId(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .hasMessage("未获取到当前用户身份");
    }
}
