package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.entity.Announcement;
import com.freight.service.AnnouncementService;
import com.freight.service.WordPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Tag(name = "公告栏")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final WordPreviewService wordPreviewService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    public Result<IPage<Announcement>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(announcementService.page(pageNum, pageSize));
    }

    @Operation(summary = "新建公告")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Announcement> create(@RequestBody Announcement announcement) {
        announcementService.create(announcement);
        return Result.success(announcement);
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcementService.update(id, announcement);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    @Operation(summary = "上传公告附件（支持多个，追加不覆盖已有附件）")
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<List<String>> uploadAttachments(@PathVariable Long id,
                                                  @RequestParam("files") List<MultipartFile> files) {
        announcementService.uploadAttachments(id, files);
        return Result.success("附件上传成功", announcementService.listAttachments(id));
    }

    @Operation(summary = "查看公告附件列表")
    @GetMapping("/{id}/attachments")
    public Result<List<String>> listAttachments(@PathVariable Long id) {
        return Result.success(announcementService.listAttachments(id));
    }

    @Operation(summary = "下载公告附件")
    @GetMapping("/{id}/attachments/{filename}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id, @PathVariable String filename) {
        Path filePath = announcementService.getAttachmentFile(id, filename);
        if (filePath == null) return ResponseEntity.notFound().build();

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            String probed = Files.probeContentType(filePath);
            if (probed != null) contentType = probed;
        } catch (IOException ignored) {
            // 无法识别类型时按通用下载处理。
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(filePath));
    }

    @Operation(summary = "删除公告附件")
    @DeleteMapping("/{id}/attachments/{filename}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public Result<List<String>> deleteAttachment(@PathVariable Long id, @PathVariable String filename) {
        announcementService.deleteAttachment(id, filename);
        return Result.success("附件删除成功", announcementService.listAttachments(id));
    }

    @Operation(summary = "预览旧版 Word 公告附件")
    @GetMapping("/{id}/attachments/{filename}/word-preview")
    public ResponseEntity<byte[]> previewWordAttachment(@PathVariable Long id, @PathVariable String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            return ResponseEntity.status(415).build();
        }
        Path filePath = announcementService.getAttachmentFile(id, filename);
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
