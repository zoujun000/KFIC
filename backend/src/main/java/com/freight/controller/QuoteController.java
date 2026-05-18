package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.dto.QuoteQueryDTO;
import com.freight.entity.FreightQuote;
import com.freight.entity.QuoteUploadLog;
import com.freight.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "散货报价")
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @Operation(summary = "上传报价Excel")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<QuoteUploadLog> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(quoteService.uploadAndParse(file));
    }

    @Operation(summary = "查询报价")
    @GetMapping
    public Result<IPage<FreightQuote>> query(QuoteQueryDTO dto) {
        return Result.success(quoteService.query(dto));
    }

    @Operation(summary = "获取国家列表")
    @GetMapping("/countries")
    public Result<List<String>> countries() {
        return Result.success(quoteService.listCountries());
    }

    @Operation(summary = "获取目的港列表")
    @GetMapping("/destinations")
    public Result<List<String>> destinations(@RequestParam(required = false) String country) {
        return Result.success(quoteService.listDestinations(country));
    }

    @Operation(summary = "上传历史记录")
    @GetMapping("/logs")
    public Result<List<QuoteUploadLog>> logs() {
        return Result.success(quoteService.uploadLogs());
    }
}
