package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.service.LogService;
import com.freight.vo.LogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogService logService;

    @Operation(summary = "查询日志列表")
    @GetMapping
    public Result<List<LogVO>> list(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return Result.success(logService.listByType(type));
        }
        return Result.success(logService.listAll());
    }
}
