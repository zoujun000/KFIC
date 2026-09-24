package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import java.time.LocalDate;
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
                .le(query.getEtdEnd() != null, FreightOrder::getEtd, query.getEtdEnd())
                .ge(query.getCreateTimeStart() != null, FreightOrder::getCreateTime,
                        query.getCreateTimeStart() == null ? null : query.getCreateTimeStart().atStartOfDay())
                .lt(query.getCreateTimeEnd() != null, FreightOrder::getCreateTime,
                        query.getCreateTimeEnd() == null ? null : query.getCreateTimeEnd().plusDays(1).atStartOfDay());

        String statuses = query.getStatuses();
        if (StringUtils.hasText(statuses)) {
            wrapper.in(FreightOrder::getStatus, (Object[]) statuses.split(","));
        }

        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, requireCurrentUserId());
        }
        return wrapper;
    }

    @Override
    public FreightOrder getById(Long id) {
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            if (!requireCurrentUserId().equals(order.getCreatedBy())) {
                throw new BusinessException("无权访问该订单");
            }
        }
        return order;
    }

    @Override
    public FreightOrder create(FreightOrderDTO dto) {
        requireExistingCustomer(dto.getCustomerId());
        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order, "createdBy");
        order.setOrderNo(snowflakeIdGenerator.nextOrderNo());
        order.setStatus("进仓");
        order.setCreatedBy(requireCurrentUserId());
        orderMapper.insert(order);
        createOrderDir(order);
        return order;
    }

    @Override
    public void update(FreightOrderDTO dto) {
        if (dto.getId() == null) throw new BusinessException("订单ID不能为空");

        FreightOrder oldOrder = orderMapper.selectById(dto.getId());
        if (oldOrder == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            if (!requireCurrentUserId().equals(oldOrder.getCreatedBy())) {
                throw new BusinessException("无权修改该订单");
            }
        }

        FreightOrder order = new FreightOrder();
        BeanUtils.copyProperties(dto, order, "createdBy");
        // 贸易方式允许清空；空值转换为空字符串，配合默认非空更新策略写回数据库
        if (dto.getTradeTerms() == null) order.setTradeTerms("");

        LambdaUpdateWrapper<FreightOrder> wrapper = new LambdaUpdateWrapper<FreightOrder>()
                .eq(FreightOrder::getId, dto.getId());
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, requireCurrentUserId());
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
            wrapper.eq(FreightOrder::getCreatedBy, requireCurrentUserId());
        }
        int rows = orderMapper.update(null, wrapper);
        if (rows == 0) throw new BusinessException("订单不存在或无权修改");
    }

    @Override
    public int updateStatusesByEta(LocalDate today) {
        LocalDate effectiveDate = today == null ? LocalDate.now() : today;
        UpdateWrapper<FreightOrder> wrapper = new UpdateWrapper<FreightOrder>()
                .le("eta", effectiveDate)
                .notIn("status", "已到港", "已提货")
                .set("status", "已到港");
        return orderMapper.update(null, wrapper);
    }

    @Override
    public void delete(Long id) {
        FreightOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtil.isAdmin()) {
            if (!requireCurrentUserId().equals(order.getCreatedBy())) {
                throw new BusinessException("无权删除该订单");
            }
        }
        int rows = orderMapper.deleteById(id);
        if (rows == 0) throw new BusinessException("删除失败");
        // 有意不清理磁盘附件目录：订单为逻辑删除，文件保留便于恢复或人工归档；
        // 代价是删除后附件目录成为孤儿文件，需人工清理
        // （行为由 FreightOrderServiceImplSecurityTest.deletingOrderKeepsAttachmentsForLogicalDeletion 锁定）
    }

    @Override
    public List<FreightOrder> getEtaAlerts() {
        LambdaQueryWrapper<FreightOrder> wrapper = new LambdaQueryWrapper<FreightOrder>()
                .isNotNull(FreightOrder::getEta)
                .apply("DATE_ADD(eta, INTERVAL 1 DAY) <= CURDATE()")
                .ne(FreightOrder::getStatus, "已提货")
                .orderByAsc(FreightOrder::getEta);
        if (!SecurityUtil.isAdmin()) {
            wrapper.eq(FreightOrder::getCreatedBy, requireCurrentUserId());
        }
        return orderMapper.selectList(wrapper);
    }

    // ==================== 附件管理 ====================

    @Override
    public Path getAttachmentDir(Long orderId) {
        FreightOrder order = getById(orderId);
        Customer customer = customerMapper.selectByIdIncludeDeleted(order.getCustomerId());
        if (customer == null) throw new BusinessException("客户不存在");
        return attachmentPathService.resolveOrderDir(customer, order);
    }

    /** 校验客户存在（排除已逻辑删除），避免订单挂在无效客户上 */
    private void requireExistingCustomer(Long customerId) {
        if (customerId == null || customerMapper.selectById(customerId) == null) {
            throw new BusinessException("客户不存在");
        }
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) throw new BusinessException("未获取到当前用户身份");
        return userId;
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
