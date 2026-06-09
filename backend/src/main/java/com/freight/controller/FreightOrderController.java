package com.freight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.common.result.Result;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;
import com.freight.mapper.CustomerMapper;
import com.freight.service.FreightOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class FreightOrderController {

    private final FreightOrderService orderService;
    private final CustomerMapper customerMapper;

    // 附件存放根路径
    private static final String ATTACHMENT_ROOT = System.getProperty("user.home") + "/Desktop";

    /** 根据客户类型返回营业执照目录名 */
    private String getLicenseDir(Customer customer) {
        if ("COLOAD".equalsIgnoreCase(customer.getCustomerType())) {
            return "同行营业执照";
        }
        return "直客营业执照";
    }

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
    public Result<Void> create(@Valid @RequestBody FreightOrderDTO dto) {
        orderService.create(dto);
        return Result.success();
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
    public Result<java.util.List<FreightOrder>> etaAlerts() {
        return Result.success(orderService.getEtaAlerts());
    }

    @Operation(summary = "上传订单附件")
    @PostMapping("/{id}/attachments")
    public Result<List<String>> uploadAttachments(@PathVariable Long id,
                                                   @RequestParam("files") List<MultipartFile> files) {
        FreightOrder order = orderService.getById(id);
        if (order == null) return Result.error("订单不存在");

        // 获取客户公司名
        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null) return Result.error("客户不存在");

        String companyName = sanitizeFileName(customer.getCompanyName());
        String orderSo = sanitizeFileName(order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo());

        // 构建目标目录: /Desktop/{直客|同行}营业执照/{公司名}/{SO号}/
        Path targetDir = Paths.get(ATTACHMENT_ROOT, getLicenseDir(customer), companyName, orderSo);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            return Result.error("创建目录失败: " + e.getMessage());
        }

        List<String> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.isBlank()) continue;
                Path targetPath = targetDir.resolve(sanitizeFileName(fileName));
                file.transferTo(targetPath.toFile());
                savedFiles.add(fileName);
            } catch (IOException e) {
                return Result.error("文件保存失败: " + e.getMessage());
            }
        }
        return Result.success("上传成功", savedFiles);
    }

    @Operation(summary = "查看订单附件列表")
    @GetMapping("/{id}/attachments")
    public Result<List<String>> getAttachments(@PathVariable Long id) {
        FreightOrder order = orderService.getById(id);
        if (order == null) return Result.error("订单不存在");

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null) return Result.error("客户不存在");

        String companyName = sanitizeFileName(customer.getCompanyName());
        String orderSo = sanitizeFileName(order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo());

        Path targetDir = Paths.get(ATTACHMENT_ROOT, getLicenseDir(customer), companyName, orderSo);
        File dir = targetDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return Result.success(Collections.emptyList());
        }

        File[] files = dir.listFiles();
        if (files == null) return Result.success(Collections.emptyList());

        List<String> fileNames = Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
        return Result.success(fileNames);
    }

    @Operation(summary = "预览/下载附件")
    @GetMapping("/{id}/attachments/{filename}")
    public ResponseEntity<Resource> getAttachment(@PathVariable Long id,
                                                   @PathVariable String filename) {
        FreightOrder order = orderService.getById(id);
        if (order == null) return ResponseEntity.notFound().build();

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null) return ResponseEntity.notFound().build();

        String companyName = sanitizeFileName(customer.getCompanyName());
        String orderSo = sanitizeFileName(order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo());

        Path filePath = Paths.get(ATTACHMENT_ROOT, getLicenseDir(customer), companyName, orderSo, sanitizeFileName(filename));
        File file = filePath.toFile();
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        // 图片和PDF直接在浏览器预览，其他文件触发下载
        String disposition = "inline";
        if (contentType != null && contentType.startsWith("image/")) {
            disposition = "inline";
        } else if ("application/pdf".equals(contentType)) {
            disposition = "inline";
        } else {
            disposition = "attachment";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .body(resource);
    }

    /** 清理文件名中的非法字符 */
    private String sanitizeFileName(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
