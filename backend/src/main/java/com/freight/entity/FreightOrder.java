package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("freight_order")
public class FreightOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSo;
    private String orderNo;

    private Long customerId;

    private String shipType;

    private String tradeTerms;

    private String origin;

    private String destination;

    private String cargoName;

    private BigDecimal cargoWeight;

    private BigDecimal cargoVolume;

    private String status;

    private LocalDate etd;

    private LocalDate eta;

    private BigDecimal totalAmount;

    private String remark;

    /** 创建人用户ID（数据隔离） */
    private Long createdBy;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
