package com.freight.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DestPortChargeExcelVO {

    @ExcelProperty("目的港")
    @ColumnWidth(22)
    private String destination;

    @ExcelProperty("中文费项")
    @ColumnWidth(20)
    private String feeNameCn;

    @ExcelProperty("英文费项")
    @ColumnWidth(30)
    private String feeNameEn;

    @ExcelProperty("货币")
    @ColumnWidth(8)
    private String currency;

    @ExcelProperty("直客金额")
    @ColumnWidth(12)
    private BigDecimal amountDirect;

    @ExcelProperty("直客单位")
    @ColumnWidth(12)
    private String unitDirect;

    @ExcelProperty("同行金额")
    @ColumnWidth(12)
    private BigDecimal amountCoload;

    @ExcelProperty("同行单位")
    @ColumnWidth(12)
    private String unitCoload;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remarks;
}
