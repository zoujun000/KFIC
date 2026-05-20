package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dest_port_charge")
public class DestPortCharge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceSheet;
    private String destination;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String feeNameCn;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String feeNameEn;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String currency;
    private String amountDirectRaw;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal amountDirect;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String unitDirect;
    private String amountColoadRaw;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal amountCoload;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String unitCoload;
    private BigDecimal minDirect;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String remarks;
    private String sourceFile;
    private LocalDate uploadDate;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
