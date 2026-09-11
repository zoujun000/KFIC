package com.freight.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 大船船期明细。一个港口的一条船期一行，ETA 按港口拆开保存。 */
@Data
@TableName("freight_vessel_schedule")
public class FreightVesselSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceFile;
    private String sourceSheet;
    private String sectionName;
    private String serviceName;
    private String portCode;
    private String rawPortCode;
    private String portName;
    private String vesselVoyage;
    private LocalDate cfsClosingDate;
    private LocalDate stuffingDate;
    private LocalDate siCutoffDate;
    private LocalDate etd;
    private LocalDate eta;
    private String status;
    private Integer sourceRowNo;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
