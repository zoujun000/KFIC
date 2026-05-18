package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("freight_quote")
public class FreightQuote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceSheet;
    private String country;
    private String destination;
    private String volumeRange;
    private BigDecimal volumeMin;
    private BigDecimal volumeMax;
    private String via;
    private Integer minCharge;
    private String ofWuchong;
    private String wuchongFirstLeg;
    private String wuchongMotherVessel;
    private String ofBeisha;
    private String beishaFirstLeg;
    private String beishaMotherVessel;
    private String ofJiaoxin;
    private String jiaoxinFirstLeg;
    private String jiaoxinMotherVessel;
    private String transitTime;
    private String carrier;
    private String remarks;
    private LocalDate validFrom;
    private LocalDate validTo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
