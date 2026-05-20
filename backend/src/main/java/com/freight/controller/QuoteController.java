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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "导出全部报价为Excel文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        List<FreightQuote> rows = quoteService.listAll();
        String html = buildQuoteExcelHtml(rows);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String filename = "散货报价_全部_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xls";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(bytes);
    }

    private String buildQuoteExcelHtml(List<FreightQuote> rows) {
        String[] headers = {"国家","目的港","代码","体积区间","中转",
                "乌冲OF","乌冲头程","乌冲大船","北沙OF","北沙头程","北沙大船",
                "滘心OF","滘心头程","滘心大船","时效","船公司","有效期从","有效期至","备注"};
        StringBuilder sb = new StringBuilder();
        sb.append("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:x='urn:schemas-microsoft-com:office:excel'>");
        sb.append("<head><meta charset='UTF-8'></head><body>");
        sb.append("<table border='1'>");
        sb.append("<tr><td colspan='").append(headers.length).append("' style='font-size:14px;font-weight:bold;text-align:center;background:#4472C4;color:#fff'>崴航（广州）国际货运代理有限公司 - 散货报价表</td></tr>");
        sb.append("<tr style='background:#4472C4;color:#fff;font-weight:bold'>");
        for (String h : headers) sb.append("<td>").append(h).append("</td>");
        sb.append("</tr>");
        for (FreightQuote r : rows) {
            sb.append("<tr>");
            sb.append(td(r.getCountry())).append(td(r.getDestination())).append(td(r.getPortCode()));
            sb.append(td(r.getVolumeRange())).append(td(r.getVia()));
            sb.append(td(r.getOfWuchong())).append(td(r.getWuchongFirstLeg())).append(td(r.getWuchongMotherVessel()));
            sb.append(td(r.getOfBeisha())).append(td(r.getBeishaFirstLeg())).append(td(r.getBeishaMotherVessel()));
            sb.append(td(r.getOfJiaoxin())).append(td(r.getJiaoxinFirstLeg())).append(td(r.getJiaoxinMotherVessel()));
            sb.append(td(r.getTransitTime())).append(td(r.getCarrier()));
            sb.append(td(r.getValidFrom())).append(td(r.getValidTo())).append(td(r.getRemarks()));
            sb.append("</tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String td(Object val) {
        if (val == null) return "<td></td>";
        String s = val.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<td>" + s + "</td>";
    }
}
