package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("quote_upload_log")
public class QuoteUploadLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer total;
    private Integer inserted;
    private Integer updated;
    private Integer unchanged;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
