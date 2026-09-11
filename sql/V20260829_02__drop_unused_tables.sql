-- 删除已废弃且无数据、无业务代码引用的历史表。
-- 说明：当前目的港费用上传日志使用 port_charge_upload_log；
-- freight_rate 已由 freight_quote 替代。
USE KFIC;

DROP TABLE IF EXISTS `freight_rate`;
DROP TABLE IF EXISTS `dest_upload_log`;
