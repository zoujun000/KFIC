package com.freight.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DestChargeResultVO {

    /** 目的港名称 */
    private String portName;

    /** 费用明细列表 */
    private List<ChargeItem> items;

    /** 按货币分组的合计（汇率不换算）*/
    private Map<String, BigDecimal> totalByCurrency;

    /** 合计描述（展示用）*/
    private String totalDesc;

    @Data
    public static class ChargeItem {
        private String itemCn;
        private String itemEn;
        private String currency;
        private String rateStr;
        private BigDecimal rateNum;
        private String unit;
        private BigDecimal calcAmount;   // = rateNum × volume（计算后金额）
        private String calcDesc;         // 计算说明，如 "5.5 × 2CBM = 11"
        private String remarks;
    }
}
