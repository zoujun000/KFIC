-- 为已有订单增加收费重（计费重量）字段，单位：KG
ALTER TABLE `freight_order`
    ADD COLUMN `chargeable_weight` DECIMAL(10,2) DEFAULT NULL COMMENT '收费重(KG)'
    AFTER `cargo_weight`;
