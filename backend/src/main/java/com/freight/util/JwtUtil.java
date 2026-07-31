package com.freight.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** 生成 Access Token（短有效期，2小时） */
    public String generateAccessToken(String username, Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + accessExpiration);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 生成 Refresh Token（长有效期，30天），返回 JWT + UUID 对 */
    public RefreshTokenPair generateRefreshToken(Long userId) {
        // UUID 作为 token 标识，存 Redis
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        // JWT 也生成一个用于校验
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + refreshExpiration);
        String jwt = Jwts.builder()
                .claim("userId", userId)
                .claim("tokenId", tokenId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        return new RefreshTokenPair(tokenId, jwt);
    }

    /** 从 Refresh Token JWT 中解析 userId */
    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = getClaims(token);
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("非法的 token 类型");
        }
        Object userId = claims.get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        throw new JwtException("token 中缺少 userId");
    }

    /** 从 Refresh Token JWT 中解析 tokenId */
    public String getTokenIdFromRefreshToken(String token) {
        return (String) getClaims(token).get("tokenId");
    }

    /** 刷新 token 剩余的过期毫秒数 */
    public long getRemainingMs(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = getClaims(token).get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        return null;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Refresh Token 对（UUID + JWT） */
    public record RefreshTokenPair(String tokenId, String jwt) {}
}
