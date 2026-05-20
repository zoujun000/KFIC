package com.freight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long id;
    private String orderSo;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private String shipType;
    private String tradeTerms;
    private String origin;
    private String destination;
    private String cargoName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDate etd;
    private LocalDate eta;
    private String updateTime;
}
