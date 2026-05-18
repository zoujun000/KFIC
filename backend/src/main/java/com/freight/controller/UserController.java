package com.freight.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.common.result.Result;
import com.freight.entity.SysUser;
import com.freight.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
public class UserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setRole(body.get("role"));
        userMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "修改状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setStatus(((Number) body.get("status")).intValue());
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
