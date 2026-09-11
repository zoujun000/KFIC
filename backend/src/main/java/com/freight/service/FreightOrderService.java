package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface FreightOrderService {
    IPage<FreightOrder> page(OrderQueryDTO query);
    FreightOrder getById(Long id);
    FreightOrder create(FreightOrderDTO dto);
    void update(FreightOrderDTO dto);
    void updateStatus(Long id, String status);
    /** 将 ETA 已到达且尚未提货的订单自动更新为“已到港” */
    int updateStatusesByEta(LocalDate today);
    void delete(Long id);

    /** 查询 ETA 已过 1 天且未提货的订单（到港提醒） */
    java.util.List<FreightOrder> getEtaAlerts();

    // ==================== 附件管理 ====================

    /** 获取订单的附件目录路径（同时校验订单和客户存在性） */
    Path getAttachmentDir(Long orderId);

    /** 上传附件，返回已保存的文件名列表 */
    List<String> uploadAttachments(Long orderId, List<MultipartFile> files);

    /** 列出订单附件文件名 */
    List<String> listAttachments(Long orderId);

    /** 获取附件文件的绝对路径（用于下载/预览，不存在返回 null） */
    Path getAttachmentFile(Long orderId, String filename);
}
