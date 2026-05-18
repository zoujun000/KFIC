package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("port_charge_upload_log")
public class PortChargeUploadLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private LocalDate uploadDate;
    private Integer total;
    private Integer inserted;
    private Integer updated;
    private Integer unchanged;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
