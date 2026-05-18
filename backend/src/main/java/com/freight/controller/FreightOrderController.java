package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;
import com.freight.service.FreightOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class FreightOrderController {

    private final FreightOrderService orderService;

    @Operation(summary = "分页查询订单")
    @GetMapping
    public Result<IPage<FreightOrder>> page(OrderQueryDTO query) {
        return Result.success(orderService.page(query));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<FreightOrder> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @Operation(summary = "新建订单")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody FreightOrderDTO dto) {
        orderService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改订单")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody FreightOrderDTO dto) {
        orderService.update(dto);
        return Result.success();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.success();
    }
}
