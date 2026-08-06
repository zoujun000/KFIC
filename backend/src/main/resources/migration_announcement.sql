CREATE TABLE IF NOT EXISTS `announcement` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`           VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content`         TEXT         NOT NULL COMMENT '公告内容',
    `attachment_name` VARCHAR(255) DEFAULT NULL COMMENT '附件原始名称',
    `attachment_path` VARCHAR(255) DEFAULT NULL COMMENT '附件存储名称',
    `published_by`    BIGINT       DEFAULT NULL COMMENT '发布管理员ID',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告栏';
