-- 客户管理模块修复 — 数据库迁移SQL
-- 1. 统一 customer_type 为小写（与前端保持一致）
UPDATE customer SET customer_type = LOWER(customer_type) WHERE customer_type != LOWER(customer_type);

-- 2. 修改默认值为小写
ALTER TABLE customer ALTER COLUMN customer_type SET DEFAULT 'direct';

-- 3. 扩展 customer_code 长度以容纳随机后缀（原 VARCHAR(30) 可能不够）
-- CUS + 14位时间戳 + 4位随机数 = 21 字符，VARCHAR(30) 够用，无需修改
