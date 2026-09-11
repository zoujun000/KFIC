package com.freight.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vessel_schedule_upload_log")
public class VesselScheduleUploadLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private Integer total;
    private Integer inserted;
    private Integer updated;
    private Integer unchanged;
    private Integer removed;
    private Integer skipped;
    private String warnings;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
