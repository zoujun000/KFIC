-- 订单管理 bug 修复 — 数据库迁移SQL
-- 执行前请先连接 KFIC 数据库

ALTER TABLE freight_order
    ADD COLUMN trade_terms VARCHAR(20) COMMENT '贸易方式(FOB/CIF/EXW/DDP/DAP/CFR)' AFTER ship_type;
