-- 创建数据库
CREATE DATABASE IF NOT EXISTS freight_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE freight_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 客户表
CREATE TABLE IF NOT EXISTS `customer` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `customer_code` VARCHAR(30)  NOT NULL COMMENT '客户编号',
    `company_name`  VARCHAR(100) NOT NULL COMMENT '公司名称',
    `contact_name`  VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `address`       VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_code` (`customer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 订单表
CREATE TABLE IF NOT EXISTS `freight_order` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`        VARCHAR(30)  NOT NULL COMMENT '订单编号',
    `customer_id`     BIGINT       NOT NULL COMMENT '客户ID',
    `ship_type`       VARCHAR(20)  NOT NULL COMMENT '运输方式: SEA/AIR/LAND',
    `origin`          VARCHAR(100) NOT NULL COMMENT '起运地',
    `destination`     VARCHAR(100) NOT NULL COMMENT '目的地',
    `cargo_name`      VARCHAR(100) DEFAULT NULL COMMENT '货物名称',
    `cargo_weight`    DECIMAL(10,2) DEFAULT NULL COMMENT '货物重量(KG)',
    `cargo_volume`    DECIMAL(10,3) DEFAULT NULL COMMENT '货物体积(CBM)',
    `status`          VARCHAR(20)  NOT NULL DEFAULT '进仓' COMMENT '状态: 进仓/走船/已到港/已提货',
    `etd`             DATE         DEFAULT NULL COMMENT '预计出发日期',
    `eta`             DATE         DEFAULT NULL COMMENT '预计到达日期',
    `total_amount`    DECIMAL(12,2) DEFAULT NULL COMMENT '总金额',
    `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_by`      VARCHAR(50)  DEFAULT NULL COMMENT '创建人用户名',
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货运订单表';

-- 初始管理员账号 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'ADMIN', 1);
