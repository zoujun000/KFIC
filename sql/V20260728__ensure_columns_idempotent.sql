-- =====================================================================
-- 幂等列补齐脚本（可重复执行，已存在的列自动跳过，不修改已有列类型，不删数据）
-- 作用：按实体字段对齐 freight_order / customer / freight_quote 三张表，
--       一次性消除 "Unknown column 'xxx' in 'field list'" 类报错。
-- 执行方式：先选中目标库（默认 KFIC，如库名不同请修改下一行），整段执行。
--   在 IntelliJ Database / DataGrip / Navicat / mysql 命令行均可（支持 DELIMITER）。
-- 说明：所有新增列均为 NULLable、不加索引、不指定 AFTER，故对非空表也安全，
--       且列顺序不影响 MyBatis-Plus（按列名映射）。
-- =====================================================================
USE KFIC;

DELIMITER //
DROP PROCEDURE IF EXISTS ensure_col//
CREATE PROCEDURE ensure_col(IN t VARCHAR(64), IN c VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t AND COLUMN_NAME = c
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', t, '` ADD COLUMN `', c, '` ', ddl);
    PREPARE st FROM @s;
    EXECUTE st;
    DEALLOCATE PREPARE st;
  END IF;
END//
DELIMITER ;

-- ---------------- freight_order ----------------
CALL ensure_col('freight_order','order_so','VARCHAR(100) NULL');
CALL ensure_col('freight_order','order_no','VARCHAR(30) NULL');
CALL ensure_col('freight_order','customer_id','BIGINT NULL');
CALL ensure_col('freight_order','ship_type','VARCHAR(20) NULL');
CALL ensure_col('freight_order','trade_terms','VARCHAR(20) NULL');
CALL ensure_col('freight_order','origin','VARCHAR(100) NULL');
CALL ensure_col('freight_order','destination','VARCHAR(100) NULL');
CALL ensure_col('freight_order','cargo_name','VARCHAR(100) NULL');
CALL ensure_col('freight_order','cargo_weight','DECIMAL(10,2) NULL');
CALL ensure_col('freight_order','chargeable_weight','DECIMAL(10,2) NULL');
CALL ensure_col('freight_order','cargo_volume','DECIMAL(10,3) NULL');
CALL ensure_col('freight_order','package_count','INT NULL');
CALL ensure_col('freight_order','vessel_voyage','VARCHAR(100) NULL');
CALL ensure_col('freight_order','shipping_company','VARCHAR(100) NULL');
CALL ensure_col('freight_order','container_seal','VARCHAR(200) NULL');
CALL ensure_col('freight_order','status','VARCHAR(20) NULL');
CALL ensure_col('freight_order','etd','DATE NULL');
CALL ensure_col('freight_order','eta','DATE NULL');
CALL ensure_col('freight_order','total_amount','DECIMAL(12,2) NULL');
CALL ensure_col('freight_order','remark','VARCHAR(500) NULL');
CALL ensure_col('freight_order','created_by','BIGINT NULL');

-- ---------------- customer ----------------
CALL ensure_col('customer','customer_code','VARCHAR(30) NULL');
CALL ensure_col('customer','company_name','VARCHAR(100) NULL');
CALL ensure_col('customer','contact_name','VARCHAR(50) NULL');
CALL ensure_col('customer','phone','VARCHAR(20) NULL');
CALL ensure_col('customer','email','VARCHAR(100) NULL');
CALL ensure_col('customer','address','VARCHAR(255) NULL');
CALL ensure_col('customer','wechat','VARCHAR(100) NULL');
CALL ensure_col('customer','whatsapp','VARCHAR(100) NULL');
CALL ensure_col('customer','remark','VARCHAR(500) NULL');
CALL ensure_col('customer','photo_url','VARCHAR(500) NULL');
CALL ensure_col('customer','license_uploaded_by','BIGINT NULL');
CALL ensure_col('customer','created_by','BIGINT NULL');
CALL ensure_col('customer','status','INT NULL');
CALL ensure_col('customer','customer_type','VARCHAR(20) NULL');

-- ---------------- freight_quote ----------------
CALL ensure_col('freight_quote','source_sheet','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','country','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','destination','VARCHAR(200) NULL');
CALL ensure_col('freight_quote','volume_range','VARCHAR(50) NULL');
CALL ensure_col('freight_quote','volume_min','DECIMAL(10,3) NULL');
CALL ensure_col('freight_quote','volume_max','DECIMAL(10,3) NULL');
CALL ensure_col('freight_quote','via','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','min_charge','INT NULL');
CALL ensure_col('freight_quote','of_wuchong','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','wuchong_first_leg','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','wuchong_mother_vessel','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','of_beisha','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','beisha_first_leg','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','beisha_mother_vessel','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','of_jiaoxin','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','jiaoxin_first_leg','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','jiaoxin_mother_vessel','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','transit_time','VARCHAR(50) NULL');
CALL ensure_col('freight_quote','carrier','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','vessel_voyage','VARCHAR(100) NULL');
CALL ensure_col('freight_quote','remarks','VARCHAR(500) NULL');
CALL ensure_col('freight_quote','port_code','VARCHAR(10) NULL');
CALL ensure_col('freight_quote','valid_from','DATE NULL');
CALL ensure_col('freight_quote','valid_to','DATE NULL');

-- 清理临时过程
DROP PROCEDURE IF EXISTS ensure_col;
