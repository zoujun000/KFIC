package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;
import com.freight.mapper.CustomerMapper;
import com.freight.mapper.FreightOrderMapper;
import com.freight.service.AttachmentPathService;
import com.freight.service.FreightOrderService;
import com.freight.util.SecurityUtil;
import com.freight.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreightOrderServiceImpl implements FreightOrderService {

    private final FreightOrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final AttachmentPathService attachmentPathService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public IPage<FreightOrder> page(OrderQueryDTO query) {
        LambdaQueryWrapper<FreightOrder> wrapper = buildBaseQuery(query)
                .orderByDesc(FreightOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    private LambdaQueryWrapper<FreightOrder> buildBaseQuery(OrderQueryDTO query) {
        String so = query.getOrderSo();
        if (StringUtils.hasText(so)) so = so.trim();
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .like(StringUtils.hasText(so), FreightOrder::getOrderSo, so)
                .eq(query.getCustomerId() != null, FreightOrder::getCustomerId, query.getCustomerId())
                .eq(StringUtils.hasText(query.getShipType()), FreightOrder::getShipType, query.getShipType())
                .eq(StringUtils.hasText(query.getStatus()), FreightOrder::getStatus, query.getStatus())
                .ge(query.getEtdStart() != null, FreightOrder::getEtd, query.getEtdStart())
                .le(query.getEtdEnd() != null, FreightOrder::getEtd, query.getEtdEnd());

        String statuses = query.getStatuses();
        if (StringUtils.hasText(statuses)) {
            wrapper.in(FreightOrder::getStatus, (Object[]) statuses.split(","));
        }

        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) wrapper.eq(FreightOrder::getCreatedBy, userId);
        }
        return wrapper;
    }

    @Override
    public FreightOrder getById(Long id) {
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(order.getCreatedBy())) {
                throw new BusinessException("无权访问该订单");
            }
        }
        return order;
    }

    @Override
    public void create(FreightOrderDTO dto) {
        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order);
        order.setOrderNo(snowflakeIdGenerator.nextOrderNo());
        order.setStatus("进仓");
        order.setCreatedBy(SecurityUtil.getCurrentUserId());
        orderMapper.insert(order);
        createOrderDir(order);
    }

    @Override
    public void update(FreightOrderDTO dto) {
        if (dto.getId() == null) throw new BusinessException("订单ID不能为空");

        FreightOrder oldOrder = orderMapper.selectById(dto.getId());
        if (oldOrder == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(oldOrder.getCreatedBy())) {
                throw new BusinessException("无权修改该订单");
            }
        }

        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order);

        LambdaUpdateWrapper<FreightOrder> wrapper = new LambdaUpdateWrapper<FreightOrder>()
                .eq(FreightOrder::getId, dto.getId());
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, SecurityUtil.getCurrentUserId());
        }

        int rows = orderMapper.update(order, wrapper);
        if (rows == 0) throw new BusinessException("订单不存在或无权修改");

        renameOrderDir(oldOrder, dto);
    }

    @Override
    public void updateStatus(Long id, String status) {
        LambdaUpdateWrapper<FreightOrder> wrapper = new LambdaUpdateWrapper<FreightOrder>()
                .eq(FreightOrder::getId, id)
                .set(FreightOrder::getStatus, status);
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, SecurityUtil.getCurrentUserId());
        }
        int rows = orderMapper.update(null, wrapper);
        if (rows == 0) throw new BusinessException("订单不存在或无权修改");
    }

    @Override
    public void delete(Long id) {
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null && !userId.equals(order.getCreatedBy())) {
                throw new BusinessException("无权删除该订单");
            }
        }
        deleteOrderDir(order);
        int rows = orderMapper.deleteById(id);
        if (rows == 0) throw new BusinessException("删除失败");
    }

    @Override
    public List<FreightOrder> getEtaAlerts() {
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .isNotNull(FreightOrder::getEta)
                .apply("DATE_ADD(eta, INTERVAL 1 DAY) <= CURDATE()")
                .ne(FreightOrder::getStatus, "已提货")
                .orderByAsc(FreightOrder::getEta);
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) wrapper.eq(FreightOrder::getCreatedBy, userId);
        }
        return orderMapper.selectList(wrapper);
    }

    // ==================== 附件管理 ====================

    @Override
    public Path getAttachmentDir(Long orderId) {
        FreightOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        Customer customer = customerMapper.selectByIdIncludeDeleted(order.getCustomerId());
        if (customer == null) throw new BusinessException("客户不存在");
        return attachmentPathService.resolveOrderDir(customer, order);
    }

    @Override
    public List<String> uploadAttachments(Long orderId, List<MultipartFile> files) {
        Path targetDir = getAttachmentDir(orderId);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new BusinessException("创建目录失败: " + e.getMessage());
        }

        List<String> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) continue;
            try {
                Path targetPath = targetDir.resolve(attachmentPathService.sanitizeFileName(fileName));
                file.transferTo(targetPath.toFile());
                savedFiles.add(fileName);
            } catch (IOException e) {
                throw new BusinessException("文件保存失败: " + e.getMessage());
            }
        }
        return savedFiles;
    }

    @Override
    public List<String> listAttachments(Long orderId) {
        Path targetDir = getAttachmentDir(orderId);
        File dir = targetDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        File[] files = dir.listFiles();
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public Path getAttachmentFile(Long orderId, String filename) {
        Path dir = getAttachmentDir(orderId);
        Path filePath = dir.resolve(attachmentPathService.sanitizeFileName(filename));
        return filePath.toFile().exists() ? filePath : null;
    }

    // ==================== 文件系统管理（内部） ====================

    private void createOrderDir(FreightOrder order) {
        try {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer == null) return;
            Path dir = attachmentPathService.resolveOrderDir(customer, order);
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // 创建失败不阻断业务流程
        }
    }

    private void deleteOrderDir(FreightOrder order) {
        try {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer == null) return;
            Path dir = attachmentPathService.resolveOrderDir(customer, order);
            deleteRecursively(dir);
        } catch (IOException ignored) {
            // 删除失败不阻断业务流程
        }
    }

    private void renameOrderDir(FreightOrder oldOrder, FreightOrderDTO newOrder) {
        try {
            Customer oldCustomer = customerMapper.selectById(oldOrder.getCustomerId());
            if (oldCustomer == null) return;
            Path oldDir = attachmentPathService.resolveOrderDir(oldCustomer, oldOrder);
            if (!Files.exists(oldDir)) return;

            Long newCustomerId = newOrder.getCustomerId() != null ? newOrder.getCustomerId() : oldOrder.getCustomerId();
            Customer newCustomer = customerMapper.selectById(newCustomerId);
            if (newCustomer == null) return;

            String fallbackSo = oldOrder.getOrderSo() != null ? oldOrder.getOrderSo() : oldOrder.getOrderNo();
            String newSo = newOrder.getOrderSo() != null ? newOrder.getOrderSo() : fallbackSo;
            Path newDir = attachmentPathService.resolveOrderDir(newCustomer, newSo, oldOrder.getOrderNo());

            if (oldDir.equals(newDir)) return;

            if (Files.exists(newDir)) {
                try (var stream = Files.list(oldDir)) {
                    for (Path src : stream.toList()) {
                        Path dest = newDir.resolve(src.getFileName().toString());
                        if (!Files.exists(dest)) Files.move(src, dest);
                    }
                }
                deleteRecursively(oldDir);
            } else {
                Files.createDirectories(newDir.getParent());
                Files.move(oldDir, newDir);
            }
        } catch (IOException ignored) {
            // 重命名失败不阻断业务流程
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
