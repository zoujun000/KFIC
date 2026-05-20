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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @Operation(summary = "导出全部目的港费用为Excel文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        List<DestPortCharge> rows = chargeService.listAll();
        String html = buildPortChargeExcelHtml(rows);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String filename = "目的港费用_全部_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xls";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(bytes);
    }

    private String buildPortChargeExcelHtml(List<DestPortCharge> rows) {
        String[] headers = {"目的港","中文费项","英文费项","货币","直客金额","直客单位","同行金额","同行单位","备注"};
        StringBuilder sb = new StringBuilder();
        sb.append("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:x='urn:schemas-microsoft-com:office:excel'>");
        sb.append("<head><meta charset='UTF-8'></head><body>");
        sb.append("<table border='1'>");
        sb.append("<tr><td colspan='").append(headers.length).append("' style='font-size:14px;font-weight:bold;text-align:center;background:#4472C4;color:#fff'>崴航（广州）国际货运代理有限公司 - 目的港费用表</td></tr>");
        sb.append("<tr style='background:#4472C4;color:#fff;font-weight:bold'>");
        for (String h : headers) sb.append("<td>").append(h).append("</td>");
        sb.append("</tr>");
        for (DestPortCharge r : rows) {
            sb.append("<tr>");
            sb.append(td(r.getDestination())).append(td(r.getFeeNameCn())).append(td(r.getFeeNameEn()));
            sb.append(td(r.getCurrency())).append(td(r.getAmountDirect())).append(td(r.getUnitDirect()));
            sb.append(td(r.getAmountCoload())).append(td(r.getUnitCoload())).append(td(r.getRemarks()));
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
