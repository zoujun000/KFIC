package com.freight.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteQueryDTO {
    private String country;
    private String destination;
    private BigDecimal volume;   // 实际体积，用于匹配区间
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
