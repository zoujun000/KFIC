-- 散货报价表解析修复 — 新增头程/大船字段
ALTER TABLE freight_quote
    ADD COLUMN wuchong_first_leg VARCHAR(50) COMMENT '乌冲头程' AFTER of_wuchong,
    ADD COLUMN wuchong_mother_vessel VARCHAR(50) COMMENT '乌冲大船' AFTER wuchong_first_leg,
    ADD COLUMN beisha_first_leg VARCHAR(50) COMMENT '北沙头程' AFTER of_beisha,
    ADD COLUMN beisha_mother_vessel VARCHAR(50) COMMENT '北沙大船' AFTER beisha_first_leg,
    ADD COLUMN jiaoxin_first_leg VARCHAR(50) COMMENT '滘心头程' AFTER of_jiaoxin,
    ADD COLUMN jiaoxin_mother_vessel VARCHAR(50) COMMENT '滘心大船' AFTER jiaoxin_first_leg;
