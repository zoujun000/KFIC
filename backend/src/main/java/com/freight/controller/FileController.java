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

    // 营业执照基础路径
    private static final String BASE_DIR = System.getProperty("user.home") + "/Desktop";

    // 允许的图片类型
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");

    @Operation(summary = "上传营业执照（支持图片和PDF）")
    @PostMapping("/upload/business-license")
    public Result<String> uploadBusinessLicense(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customerType", required = false, defaultValue = "DIRECT") String customerType,
            @RequestParam(value = "companyName", required = false) String companyName) {

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
            // 确定子目录: 直客营业执照 或 同行营业执照
            String typeDir = "DIRECT".equals(customerType) ? "直客营业执照" : "同行营业执照";

            // 公司名作为子文件夹（清理非法字符）
            String safeCompanyName = sanitizeFolderName(companyName);
            if (safeCompanyName == null || safeCompanyName.isEmpty()) {
                safeCompanyName = "未分类";
            }

            // 目标路径: ~/Desktop/{直客营业执照|同行营业执照}/{公司名}/
            Path targetDir = Paths.get(BASE_DIR, typeDir, safeCompanyName);
            File dir = targetDir.toFile();
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
            Path targetPath = targetDir.resolve(newFileName);
            file.transferTo(targetPath.toFile());

            // 返回相对路径: 直客营业执照/公司名/文件名
            String relativePath = typeDir + "/" + safeCompanyName + "/" + newFileName;
            return Result.success("上传成功", relativePath);

        } catch (IOException e) {
            return Result.error("文件保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查看营业执照照片")
    @GetMapping("/photo/{*filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable("filename") String filename) {
        // filename 可能是 "直客营业执照/JOJO/xxx.pdf" 格式的相对路径
        Path filePath = Paths.get(BASE_DIR, filename);
        File file = filePath.toFile();

        // 向后兼容：老格式仅文件名，尝试在直客营业执照和同行营业执照下查找
        if (!file.exists() && !filename.contains("/")) {
            String[] dirs = {"直客营业执照", "同行营业执照"};
            for (String dir : dirs) {
                Path searchDir = Paths.get(BASE_DIR, dir);
                if (Files.exists(searchDir)) {
                    try {
                        java.util.Optional<Path> found = Files.walk(searchDir, 3)
                                .filter(p -> p.getFileName().toString().equals(filename))
                                .findFirst();
                        if (found.isPresent()) {
                            file = found.get().toFile();
                            break;
                        }
                    } catch (IOException ignored) {}
                }
            }
        }

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentTypeStr;
        try {
            contentTypeStr = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentTypeStr = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentTypeStr != null ? contentTypeStr : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    /**
     * 清理公司名中不适合做文件夹名的字符
     */
    private String sanitizeFolderName(String name) {
        if (name == null) return null;
        // macOS: 不能有 : 和 /
        // 同时去掉首尾空格
        return name.replaceAll("[/:]", "_").trim();
    }
}
