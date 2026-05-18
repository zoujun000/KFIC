-- 订单数据隔离 — 数据库迁移SQL
-- 执行前请先连接 KFIC 数据库

ALTER TABLE freight_order
    ADD COLUMN created_by VARCHAR(50) COMMENT '创建人用户名' AFTER remark;
