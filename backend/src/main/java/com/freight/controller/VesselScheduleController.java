package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.entity.VesselScheduleUploadLog;
import com.freight.service.VesselScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "大船船期")
@RestController
@RequestMapping("/api/vessel-schedules")
@RequiredArgsConstructor
public class VesselScheduleController {

    private final VesselScheduleService scheduleService;

    @Operation(summary = "上传大船船期 Excel")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<VesselScheduleUploadLog> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(scheduleService.uploadAndParse(file));
    }

    @Operation(summary = "大船船期上传历史")
    @GetMapping("/logs")
    public Result<List<VesselScheduleUploadLog>> logs() {
        return Result.success(scheduleService.uploadLogs());
    }
}
