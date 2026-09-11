package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.common.exception.BusinessException;
import com.freight.dto.LoginDTO;
import com.freight.dto.RefreshTokenDTO;
import com.freight.dto.RegisterDTO;
import com.freight.entity.SysUser;
import com.freight.mapper.SysUserMapper;
import com.freight.service.AuthService;
import com.freight.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_PREFIX = "refresh_token:";
    private static final String REFRESH_USER_KEY = "refresh_user:";
    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    /** 连续失败达到该次数后锁定账号 */
    private static final int MAX_LOGIN_FAILS = 5;
    private static final long LOGIN_LOCK_MINUTES = 15;

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        // A6 限流：锁定期间直接拒绝，不消耗查库与 bcrypt 资源
        String failKey = LOGIN_FAIL_PREFIX + dto.getUsername();
        Long fails = getLoginFails(failKey);
        if (fails != null && fails >= MAX_LOGIN_FAILS) {
            throw new BusinessException(429, "登录失败次数过多，请 " + LOGIN_LOCK_MINUTES + " 分钟后再试");
        }

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getDeleted, 0)
        );
        // 用户不存在也计入失败次数，避免响应差异被用于用户名枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误" + recordLoginFail(failKey));
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        clearLoginFails(failKey);
        return buildTokenResponse(user);
    }

    /** 读取当前连续失败次数；Redis 不可用时 fail-open 放行登录 */
    private Long getLoginFails(String failKey) {
        try {
            Object value = redisTemplate.opsForValue().get(failKey);
            return value == null ? null : Long.valueOf(value.toString());
        } catch (Exception e) {
            log.warn("登录失败计数读取不可用: {}", e.getMessage());
            return null;
        }
    }

    /** 记录一次失败并返回提示文案（剩余次数或锁定提示） */
    private String recordLoginFail(String failKey) {
        try {
            Long count = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            if (count == null) return "";
            if (count >= MAX_LOGIN_FAILS) {
                return "，账号已锁定 " + LOGIN_LOCK_MINUTES + " 分钟";
            }
            return "，还可尝试 " + (MAX_LOGIN_FAILS - count) + " 次";
        } catch (Exception e) {
            log.warn("登录失败计数不可用: {}", e.getMessage());
            return "";
        }
    }

    /** 登录成功清零失败计数 */
    private void clearLoginFails(String failKey) {
        try {
            redisTemplate.delete(failKey);
        } catch (Exception e) {
            log.warn("清除登录失败计数不可用: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refresh(RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();

        // 1. 校验 JWT 签名和过期
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "refresh token 无效或已过期，请重新登录");
        }

        // 2. 解析 userId 和 tokenId
        Long userId;
        String tokenId;
        try {
            userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
            tokenId = jwtUtil.getTokenIdFromRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(401, "refresh token 格式错误");
        }

        // 3. 检查 Redis 中是否存在该 token
        String redisKey = REFRESH_PREFIX + userId + ":" + tokenId;
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.FALSE.equals(exists)) {
            // token 已被使用或撤销，可能是重复刷新攻击
            // 防御：同时吊销该用户当前有效的 refresh token，所有设备需重新登录
            String userKey = REFRESH_USER_KEY + userId;
            Object activeTokenId = redisTemplate.opsForValue().get(userKey);
            if (activeTokenId != null) {
                redisTemplate.delete(REFRESH_PREFIX + userId + ":" + activeTokenId);
            }
            redisTemplate.delete(userKey);
            throw new BusinessException(401, "refresh token 已被使用，请重新登录");
        }

        // 4. 查用户状态
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .eq(SysUser::getDeleted, 0)
        );
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用或不存在");
        }

        // 5. 删除旧 token，生成新的（token 轮转）
        redisTemplate.delete(redisKey);
        return buildTokenResponse(user);
    }

    @Override
    public void register(RegisterDTO dto) {
        // A4 说明：查重包含已删除账号。username 带唯一索引（uk_username），已删除账号仍占用用户名，
        // 避免同名账号在历史上出现多行；如需复用已删除用户名，需先调整表结构
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
        );
        if (exist != null) {
            throw new BusinessException("该账号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole("USER");
        user.setStatus(1);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册竞态：两者都通过查重后撞 username 唯一索引
            throw new BusinessException("该账号已被注册");
        }
    }

    private Map<String, Object> buildTokenResponse(SysUser user) {
        // 1. 生成 Access Token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());

        // 2. 生成 Refresh Token + 存 Redis
        JwtUtil.RefreshTokenPair pair = jwtUtil.generateRefreshToken(user.getId());
        String redisKey = REFRESH_PREFIX + user.getId() + ":" + pair.tokenId();

        // 删除该用户旧的 refresh token 本体与指针（单设备登录：旧设备的 token 立即失效）
        String userKey = REFRESH_USER_KEY + user.getId();
        Object oldTokenId = redisTemplate.opsForValue().get(userKey);
        if (oldTokenId != null) {
            redisTemplate.delete(REFRESH_PREFIX + user.getId() + ":" + oldTokenId);
        }
        redisTemplate.delete(userKey);

        // 存新 token，过期时间与 JWT 一致
        long ttl = jwtUtil.getRemainingMs(pair.jwt());
        redisTemplate.opsForValue().set(redisKey, "1", ttl, TimeUnit.MILLISECONDS);

        // 记录当前有效 token key（用于单设备踢下线）
        redisTemplate.opsForValue().set(userKey, pair.tokenId(), ttl, TimeUnit.MILLISECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", pair.jwt());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("userId", user.getId());
        return result;
    }
}
