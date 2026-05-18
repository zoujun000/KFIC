package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dest_upload_log")
public class DestUploadLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private Integer total;
    private Integer inserted;
    private Integer updated;
    private Integer unchanged;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
