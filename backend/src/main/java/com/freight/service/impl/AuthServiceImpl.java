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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_PREFIX = "refresh_token:";
    private static final String REFRESH_USER_KEY = "refresh_user:";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getDeleted, 0)
        );
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return buildTokenResponse(user);
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
            // 删除该用户所有 refresh token（防御措施）
            String userKey = REFRESH_USER_KEY + userId;
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
        sysUserMapper.insert(user);
    }

    private Map<String, Object> buildTokenResponse(SysUser user) {
        // 1. 生成 Access Token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());

        // 2. 生成 Refresh Token + 存 Redis
        JwtUtil.RefreshTokenPair pair = jwtUtil.generateRefreshToken(user.getId());
        String redisKey = REFRESH_PREFIX + user.getId() + ":" + pair.tokenId();

        // 删除该用户旧的 refresh token（单设备登录）
        String userKey = REFRESH_USER_KEY + user.getId();
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
