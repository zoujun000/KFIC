package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.dto.PortChargeSummaryDTO;
import com.freight.entity.DestPortCharge;
import com.freight.entity.PortChargeUploadLog;
import com.freight.service.DestPortChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "目的港费用")
@RestController
@RequestMapping("/api/port-charges")
@RequiredArgsConstructor
public class DestPortChargeController {

    private final DestPortChargeService chargeService;

    @Operation(summary = "上传目的港费用Excel")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PortChargeUploadLog> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(chargeService.uploadAndParse(file));
    }

    @Operation(summary = "计算目的港费用")
    @GetMapping("/calc")
    public Result<PortChargeSummaryDTO> calc(
            @RequestParam String destination,
            @RequestParam(defaultValue = "1") BigDecimal volume,
            @RequestParam(defaultValue = "direct") String clientType) {
        return Result.success(chargeService.calcCharges(destination, volume, clientType));
    }

    @Operation(summary = "获取目的港列表")
    @GetMapping("/destinations")
    public Result<List<String>> destinations() {
        return Result.success(chargeService.listDestinations());
    }

    @Operation(summary = "上传历史记录")
    @GetMapping("/logs")
    public Result<List<PortChargeUploadLog>> logs() {
        return Result.success(chargeService.uploadLogs());
    }

    @Operation(summary = "查询目的港费用明细（按目的港）")
    @GetMapping
    public Result<List<DestPortCharge>> list(@RequestParam String destination) {
        return Result.success(chargeService.listByDestination(destination));
    }

    @Operation(summary = "更新单条目的港费用")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody DestPortCharge charge) {
        charge.setId(id);
        chargeService.updateCharge(charge);
        return Result.success();
    }

    @Operation(summary = "删除单条目的港费用")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> deleteCharge(@PathVariable Long id) {
        chargeService.deleteCharge(id);
        return Result.success();
    }

    @Operation(summary = "导出全部目的港费用")
    @GetMapping("/all")
    public Result<List<DestPortCharge>> listAll() {
        return Result.success(chargeService.listAll());
    }
}
