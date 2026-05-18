package com.freight.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一日志视图 — 合并报价上传日志 + 目的港费用上传日志
 */
@Data
public class LogVO {

    private Long id;

    /** 日志类型：QUOTE_UPLOAD / PORT_CHARGE_UPLOAD */
    private String type;

    /** 类型显示名 */
    private String typeName;

    /** 文件名 */
    private String fileName;

    /** 总数 */
    private Integer total;

    /** 新增数 */
    private Integer inserted;

    /** 更新数 */
    private Integer updated;

    /** 未变数 */
    private Integer unchanged;

    /** 有效期起（仅报价上传有）*/
    private LocalDate validFrom;

    /** 有效期止（仅报价上传有）*/
    private LocalDate validTo;

    /** 上传/操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
