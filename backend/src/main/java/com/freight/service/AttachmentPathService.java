package com.freight.service;

import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;

import java.nio.file.Path;

/**
 * 统一管理订单附件的文件系统路径。
 * 目录结构: ~/Desktop/{直客|同行}营业执照/{公司名}/{SO号}/
 */
public interface AttachmentPathService {

    /** 根据订单和客户信息计算附件目录路径 */
    Path resolveOrderDir(Customer customer, FreightOrder order);

    /** 计算指定 SO 号的附件目录（用于重命名等场景） */
    Path resolveOrderDir(Customer customer, String orderSo, String orderNo);

    /** 清理文件名中的非法字符 */
    String sanitizeFileName(String name);

    /** 根据客户类型返回营业执照目录名 */
    String getLicenseDir(Customer customer);
}
