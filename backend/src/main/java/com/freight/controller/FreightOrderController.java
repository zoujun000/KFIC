package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;
import com.freight.service.FreightOrderService;
import com.freight.service.WordPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class FreightOrderController {

    private final FreightOrderService orderService;
    private final WordPreviewService wordPreviewService;

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
    public Result<FreightOrder> create(@Valid @RequestBody FreightOrderDTO dto) {
        return Result.success(orderService.create(dto));
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
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.success();
    }

    @Operation(summary = "查询已到港需提醒的订单（ETA+1天且未提货）")
    @GetMapping("/eta-alerts")
    public Result<List<FreightOrder>> etaAlerts() {
        return Result.success(orderService.getEtaAlerts());
    }

    // ==================== 附件管理 ====================

    @Operation(summary = "上传订单附件")
    @PostMapping("/{id}/attachments")
    public Result<List<String>> uploadAttachments(@PathVariable Long id,
                                                   @RequestParam("files") List<MultipartFile> files) {
        return Result.success("上传成功", orderService.uploadAttachments(id, files));
    }

    @Operation(summary = "查看订单附件列表")
    @GetMapping("/{id}/attachments")
    public Result<List<String>> getAttachments(@PathVariable Long id) {
        return Result.success(orderService.listAttachments(id));
    }

    @Operation(summary = "预览/下载附件")
    @GetMapping("/{id}/attachments/{filename}")
    public ResponseEntity<Resource> getAttachment(@PathVariable Long id,
                                                   @PathVariable String filename) {
        Path filePath = orderService.getAttachmentFile(id, filename);
        if (filePath == null) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(filePath);
        String contentType = "application/octet-stream";
        try {
            String probed = Files.probeContentType(filePath);
            if (probed != null) contentType = probed;
        } catch (IOException ignored) {
            // fallback to octet-stream
        }

        String disposition = (contentType.startsWith("image/") || "application/pdf".equals(contentType))
                ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(disposition)
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    @Operation(summary = "预览旧版 Word 附件")
    @GetMapping("/{id}/attachments/{filename}/word-preview")
    public ResponseEntity<byte[]> previewWordAttachment(@PathVariable Long id,
                                                        @PathVariable String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            return ResponseEntity.status(415).build();
        }

        Path filePath = orderService.getAttachmentFile(id, filename);
        if (filePath == null) return ResponseEntity.notFound().build();

        try {
            byte[] html = wordPreviewService.convertDocToHtml(filePath);
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                    .header("Content-Security-Policy",
                            "default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(html);
        } catch (IOException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
