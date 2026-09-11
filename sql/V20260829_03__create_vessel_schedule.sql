-- 大船船期独立表。上传表格中的每个港口 ETA 拆成一行。
USE KFIC;

CREATE TABLE IF NOT EXISTS `freight_vessel_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `source_file` VARCHAR(255) NOT NULL COMMENT '上传文件名',
    `source_sheet` VARCHAR(100) NOT NULL COMMENT '来源工作表',
    `section_name` VARCHAR(255) NOT NULL COMMENT '港口区块原始标题',
    `service_name` VARCHAR(150) DEFAULT NULL COMMENT '航线/船公司服务',
    `port_code` VARCHAR(20) NOT NULL COMMENT '标准港口代码',
    `raw_port_code` VARCHAR(30) DEFAULT NULL COMMENT 'Excel中的港口代码',
    `port_name` VARCHAR(150) DEFAULT NULL COMMENT '港口名称',
    `vessel_voyage` VARCHAR(200) NOT NULL COMMENT '船名航次',
    `cfs_closing_date` DATE DEFAULT NULL,
    `stuffing_date` DATE DEFAULT NULL,
    `si_cutoff_date` DATE DEFAULT NULL,
    `etd` DATE DEFAULT NULL COMMENT '开船日期',
    `eta` DATE DEFAULT NULL COMMENT '到港日期',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/TBA/SUSPEND/OTHER',
    `source_row_no` INT DEFAULT NULL COMMENT 'Excel行号',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `natural_key_hash` BINARY(16) GENERATED ALWAYS AS (
        UNHEX(MD5(CONCAT_WS('||', `source_file`, `source_sheet`, `section_name`, `port_code`, `vessel_voyage`, `etd`)))
    ) STORED COMMENT '完整业务键哈希，避免联合索引超长',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_natural_hash` (`natural_key_hash`),
    KEY `idx_schedule_port_etd` (`port_code`, `etd`, `deleted`),
    KEY `idx_schedule_source` (`source_file`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大船船期表';

CREATE TABLE IF NOT EXISTS `vessel_schedule_upload_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `file_name` VARCHAR(255) NOT NULL,
    `total` INT NOT NULL DEFAULT 0,
    `inserted` INT NOT NULL DEFAULT 0,
    `updated` INT NOT NULL DEFAULT 0,
    `unchanged` INT NOT NULL DEFAULT 0,
    `removed` INT NOT NULL DEFAULT 0 COMMENT '本次快照下线旧记录',
    `skipped` INT NOT NULL DEFAULT 0 COMMENT '无法识别或缺少ETD的行',
    `warnings` TEXT DEFAULT NULL COMMENT '导入校验提示',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大船船期上传日志';
