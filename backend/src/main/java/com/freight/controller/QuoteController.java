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
import com.alibaba.excel.EasyExcel;
import com.freight.dto.excel.FreightQuoteExcelVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @Operation(summary = "更新单条报价")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody FreightQuote quote) {
        quote.setId(id);
        quoteService.updateQuote(quote);
        return Result.success();
    }

    @Operation(summary = "删除单条报价")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        quoteService.deleteQuote(id);
        return Result.success();
    }

    @Operation(summary = "按目的港查询报价列表（无分页）")
    @GetMapping("/by-destination")
    public Result<List<FreightQuote>> listByDestination(@RequestParam String destination) {
        return Result.success(quoteService.listByDestination(destination));
    }

    @Operation(summary = "按港口缩写查询报价")
    @GetMapping("/by-port-code")
    public Result<List<FreightQuote>> listByPortCode(@RequestParam String portCode) {
        return Result.success(quoteService.listByPortCode(portCode));
    }

    @Operation(summary = "导出全部报价数据")
    @GetMapping("/all")
    public Result<List<FreightQuote>> listAll() {
        return Result.success(quoteService.listAll());
    }

    @Operation(summary = "上传历史记录")
    @GetMapping("/logs")
    public Result<List<QuoteUploadLog>> logs() {
        return Result.success(quoteService.uploadLogs());
    }

    @Operation(summary = "导出全部报价为Excel(.xlsx)")
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<FreightQuote> rows = quoteService.listAll();
        List<FreightQuoteExcelVO> vos = rows.stream().map(r -> {
            FreightQuoteExcelVO vo = new FreightQuoteExcelVO();
            org.springframework.beans.BeanUtils.copyProperties(r, vo);
            return vo;
        }).toList();

        String filename = URLEncoder.encode(
                "散货报价_全部_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx",
                StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        EasyExcel.write(response.getOutputStream(), FreightQuoteExcelVO.class)
                .sheet("散货报价")
                .doWrite(vos);
    }
}
