package com.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FreightOrderDTO {
    private String orderSo;

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotBlank(message = "运输方式不能为空")
    private String shipType;

    private String tradeTerms;

    @NotBlank(message = "起运地不能为空")
    private String origin;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    private String cargoName;
    private BigDecimal cargoWeight;
    private BigDecimal cargoVolume;
    private LocalDate etd;
    private LocalDate eta;
    private BigDecimal totalAmount;
    private String remark;
    private Long createdBy;
}
