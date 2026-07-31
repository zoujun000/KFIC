package com.freight.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.common.result.Result;
import com.freight.entity.SysUser;
import com.freight.mapper.SysUserMapper;
import com.freight.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
public class UserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "MAINTAINER", "USER");

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<List<SysUser>> list() {
        List<SysUser> users = userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreateTime));
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // 1. 禁止修改自己
        if (id.equals(SecurityUtil.getCurrentUserId())) {
            return Result.error("不能修改自己的角色");
        }

        // 2. 角色白名单校验
        String newRole = body.get("role");
        if (newRole == null || !VALID_ROLES.contains(newRole)) {
            return Result.error("无效的角色，只允许: " + String.join(", ", VALID_ROLES));
        }

        // 3. 非 ADMIN 不得授予 ADMIN 角色
        if ("ADMIN".equals(newRole) && !SecurityUtil.isAdmin()) {
            return Result.error("只有管理员才能授予管理员角色");
        }

        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setRole(newRole);
        userMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "修改状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 1. 禁止修改自己
        if (id.equals(SecurityUtil.getCurrentUserId())) {
            return Result.error("不能修改自己的状态");
        }

        // 2. 状态值校验
        Object statusObj = body.get("status");
        if (!(statusObj instanceof Number)) {
            return Result.error("status 必须是数字");
        }
        int newStatus = ((Number) statusObj).intValue();
        if (newStatus != 0 && newStatus != 1) {
            return Result.error("status 只能是 0（启用）或 1（禁用）");
        }

        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setStatus(newStatus);
        userMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-pwd")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.success();
    }
}
