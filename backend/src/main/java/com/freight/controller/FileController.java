package com.freight.controller;

import com.freight.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
public class FileController {

    // 营业执照存放路径
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/Desktop/营业执照";

    // 允许的图片类型
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");

    @Operation(summary = "上传营业执照（支持图片和PDF）")
    @PostMapping("/upload/business-license")
    public Result<String> uploadBusinessLicense(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 校验文件类型：仅允许图片或PDF
        String contentType = file.getContentType();
        if (contentType == null ||
            (!ALLOWED_IMAGE_TYPES.contains(contentType) && !"application/pdf".equals(contentType))) {
            return Result.error("仅支持图片（JPG/PNG/GIF/WebP/BMP）或 PDF 文件");
        }

        try {
            // 确保目录存在
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名：时间戳_UUID.扩展名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String newFileName = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            // 保存文件
            Path targetPath = Paths.get(UPLOAD_DIR, newFileName);
            file.transferTo(targetPath.toFile());

            return Result.success("上传成功", newFileName);
        } catch (IOException e) {
            return Result.error("文件保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查看营业执照照片")
    @GetMapping("/photo/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        Path filePath = Paths.get(UPLOAD_DIR, filename);
        File file = filePath.toFile();

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
