-- 客户管理页面改造 — 数据库迁移SQL
-- 执行前请先连接 KFIC 数据库

ALTER TABLE customer
    ADD COLUMN wechat VARCHAR(100) COMMENT '微信' AFTER phone,
    ADD COLUMN whatsapp VARCHAR(100) COMMENT 'WhatsApp' AFTER wechat,
    ADD COLUMN photo_url VARCHAR(500) COMMENT '营业执照照片路径' AFTER remark;
