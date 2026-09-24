package com.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FreightOrderDTO {

    @NotBlank(message = "SO号不能为空")
    @Size(max = 100, message = "SO号长度不能超过100")
    private String orderSo;

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotBlank(message = "运输方式不能为空")
    @Pattern(regexp = "SEA|AIR|LAND|EXPRESS", message = "运输方式必须为 SEA/AIR/LAND/EXPRESS")
    private String shipType;

    @Size(max = 20, message = "贸易方式长度不能超过20")
    private String tradeTerms;

    @NotBlank(message = "起运地不能为空")
    @Size(max = 100, message = "起运地长度不能超过100")
    private String origin;

    @NotBlank(message = "目的地不能为空")
    @Size(max = 100, message = "目的地长度不能超过100")
    private String destination;

    @Size(max = 100, message = "货物名称长度不能超过100")
    private String cargoName;

    private BigDecimal cargoWeight;
    private BigDecimal chargeableWeight;
    private BigDecimal cargoVolume;
    private Integer packageCount;

    @Size(max = 100, message = "船名航次长度不能超过100")
    private String vesselVoyage;

    @Size(max = 100, message = "船公司长度不能超过100")
    private String shippingCompany;

    @Size(max = 200, message = "柜封号长度不能超过200")
    private String containerSeal;

    private LocalDate etd;
    private LocalDate eta;
    private BigDecimal totalAmount;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
