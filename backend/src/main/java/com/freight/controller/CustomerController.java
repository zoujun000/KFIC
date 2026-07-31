package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.entity.Customer;
import com.freight.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户")
    @GetMapping
    public Result<IPage<Customer>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String customerType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(customerService.page(keyword, status, customerType, pageNum, pageSize));
    }

    @Operation(summary = "客户统计")
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        return Result.success(customerService.getStats());
    }

    @Operation(summary = "查询客户详情")
    @GetMapping("/{id:\\d+}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    public Result<Void> save(@RequestBody Customer customer) {
        customerService.save(customer);
        return Result.success();
    }

    @Operation(summary = "修改客户")
    @PutMapping
    public Result<Void> update(@RequestBody Customer customer) {
        customerService.update(customer);
        return Result.success();
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
