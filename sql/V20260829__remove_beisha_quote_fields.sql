-- 报价表仓库字段对齐：当前仅保留乌冲、滘心，删除历史北沙字段。
-- 执行前请确认不再需要 of_beisha / beisha_first_leg / beisha_mother_vessel 的历史数据。
USE KFIC;

DELIMITER //
DROP PROCEDURE IF EXISTS ensure_quote_col//
CREATE PROCEDURE ensure_quote_col(IN c VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'freight_quote'
        AND COLUMN_NAME = c
  ) THEN
    SET @s = CONCAT('ALTER TABLE `freight_quote` ADD COLUMN `', c, '` ', ddl);
    PREPARE st FROM @s;
    EXECUTE st;
    DEALLOCATE PREPARE st;
  END IF;
END//

DROP PROCEDURE IF EXISTS drop_quote_col//
CREATE PROCEDURE drop_quote_col(IN c VARCHAR(64))
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'freight_quote'
        AND COLUMN_NAME = c
  ) THEN
    SET @s = CONCAT('ALTER TABLE `freight_quote` DROP COLUMN `', c, '`');
    PREPARE st FROM @s;
    EXECUTE st;
    DEALLOCATE PREPARE st;
  END IF;
END//
DELIMITER ;

CALL ensure_quote_col('cc', 'VARCHAR(50) NULL');

-- 新版 Excel 的 T/T 允许带单位或说明，禁止因历史长度限制再次截断。
ALTER TABLE freight_quote MODIFY COLUMN transit_time VARCHAR(100) NULL;

CALL drop_quote_col('of_beisha');
CALL drop_quote_col('beisha_first_leg');
CALL drop_quote_col('beisha_mother_vessel');

DROP PROCEDURE IF EXISTS ensure_quote_col;
DROP PROCEDURE IF EXISTS drop_quote_col;
