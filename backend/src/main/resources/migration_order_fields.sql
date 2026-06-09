-- 订单表新增字段：件数、船名航次、船公司、柜封号
USE freight_db;

ALTER TABLE `freight_order`
    ADD COLUMN `package_count`      INT          DEFAULT NULL COMMENT '件数' AFTER `cargo_volume`,
    ADD COLUMN `vessel_voyage`      VARCHAR(100) DEFAULT NULL COMMENT '船名航次' AFTER `package_count`,
    ADD COLUMN `shipping_company`   VARCHAR(100) DEFAULT NULL COMMENT '船公司' AFTER `vessel_voyage`,
    ADD COLUMN `container_seal`     VARCHAR(100) DEFAULT NULL COMMENT '柜封号' AFTER `shipping_company`;

-- 散货报价表新增字段：船名航次
ALTER TABLE `freight_quote`
    ADD COLUMN `vessel_voyage`      VARCHAR(200) DEFAULT NULL COMMENT '船名航次' AFTER `carrier`;
