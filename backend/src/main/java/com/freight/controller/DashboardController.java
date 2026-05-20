package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.dto.DashboardStatsDTO;
import com.freight.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作台")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取工作台统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsDTO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
