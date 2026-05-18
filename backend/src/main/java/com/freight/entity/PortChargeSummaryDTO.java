package com.freight.entity;

import com.freight.entity.DestPortCharge;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PortChargeSummaryDTO {

    /** 目的港名称 */
    private String destination;

    /** 查询体积 (CBM) */
    private BigDecimal volume;

    /** 费用明细列表 */
    private List<DestPortCharge> items;

    /** 按货币汇总金额，key=货币，value=金额 */
    private Map<String, BigDecimal> totalByCurrency = new LinkedHashMap<>();

    /** 展示用字符串，如 "USD 150.50 + THB 3,500.00" */
    private String totalDisplay;
}
