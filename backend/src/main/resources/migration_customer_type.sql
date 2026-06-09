-- 客户表新增：客户类型（直客/同行）
USE freight_db;

ALTER TABLE `customer`
    ADD COLUMN `customer_type` VARCHAR(20) DEFAULT 'DIRECT' COMMENT '客户类型: DIRECT=直客, COLOAD=同行' AFTER `status`;
ALTER TABLE customer ADD COLUMN customer_type VARCHAR(20) DEFAULT 'DIRECT' COMMENT '客户类型' AFTER status;