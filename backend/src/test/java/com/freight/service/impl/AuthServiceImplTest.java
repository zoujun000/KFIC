package com.freight.service.impl;

import com.freight.common.exception.BusinessException;
import com.freight.dto.LoginDTO;
import com.freight.dto.RegisterDTO;
import com.freight.entity.SysUser;
import com.freight.mapper.SysUserMapper;
import com.freight.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(sysUserMapper, passwordEncoder, jwtUtil, redisTemplate);
    }

    @Test
    void newLoginInvalidatesPreviousRefreshToken() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), any())).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("access-1", "access-2");
        when(jwtUtil.generateRefreshToken(1L))
                .thenReturn(new JwtUtil.RefreshTokenPair("tidA", "jwtA"))
                .thenReturn(new JwtUtil.RefreshTokenPair("tidB", "jwtB"));
        when(jwtUtil.getRemainingMs(anyString())).thenReturn(60_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 第一次登录无旧 token；第二次登录时指针指向 tidA
        when(valueOperations.get("refresh_user:1")).thenReturn(null, "tidA");

        LoginDTO dto = new LoginDTO();
        dto.setUsername("tester");
        dto.setPassword("secret");
        service.login(dto);
        service.login(dto);

        // 第二次登录必须删除旧 token 本体，否则旧设备仍可刷新（单设备登录失效）
        verify(redisTemplate).delete("refresh_token:1:tidA");
        verify(redisTemplate, times(2)).delete("refresh_user:1");
        // 登录成功清零失败计数
        verify(redisTemplate, times(2)).delete("login_fail:tester");
    }

    @Test
    void loginLockedAfterMaxFailures() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login_fail:baduser")).thenReturn(5L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("baduser");
        dto.setPassword("whatever");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        // 锁定期间不查库不跑 bcrypt
        verifyNoInteractions(sysUserMapper, passwordEncoder);
    }

    @Test
    void failedLoginIncrementsCounterAndShowsRemaining() {
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login_fail:nobody")).thenReturn(1L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("nobody");
        dto.setPassword("wrong");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误，还可尝试 4 次");
        verify(redisTemplate).expire("login_fail:nobody", 15, TimeUnit.MINUTES);
        verify(sysUserMapper, never()).selectById(any());
    }

    @Test
    void concurrentRegisterDuplicateUsernameReturnsFriendlyError() {
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(sysUserMapper.insert(any(SysUser.class))).thenThrow(new DuplicateKeyException("uk_username"));

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("dup");
        dto.setPassword("pwd");
        dto.setRealName("重复注册");

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该账号已被注册");
    }
}
