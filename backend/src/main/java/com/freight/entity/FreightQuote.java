package com.freight.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String ofJiaoxin;
    private String jiaoxinFirstLeg;
    private String jiaoxinMotherVessel;
    private String transitTime;
    private String cc;
    private String carrier;
    private String vesselVoyage;
    private String remarks;
    private String portCode;
    private LocalDate validFrom;
    private LocalDate validTo;

    /** 查询时按目的港动态附加的未来船期，不落库。 */
    @TableField(exist = false)
    private List<FreightVesselSchedule> upcomingSchedules;

    /** 便于列表和复制报价直接使用的格式化船期文本，不落库。 */
    @TableField(exist = false)
    private String upcomingScheduleText;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
