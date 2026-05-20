package com.freight.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FreightQuoteExcelVO {

    @ExcelProperty("国家")
    @ColumnWidth(12)
    private String country;

    @ExcelProperty("目的港")
    @ColumnWidth(20)
    private String destination;

    @ExcelProperty("代码")
    @ColumnWidth(10)
    private String portCode;

    @ExcelProperty("体积区间")
    @ColumnWidth(14)
    private String volumeRange;

    @ExcelProperty("中转")
    @ColumnWidth(10)
    private String via;

    @ExcelProperty("乌冲OF")
    @ColumnWidth(10)
    private String ofWuchong;

    @ExcelProperty("乌冲头程")
    @ColumnWidth(10)
    private String wuchongFirstLeg;

    @ExcelProperty("乌冲大船")
    @ColumnWidth(10)
    private String wuchongMotherVessel;

    @ExcelProperty("北沙OF")
    @ColumnWidth(10)
    private String ofBeisha;

    @ExcelProperty("北沙头程")
    @ColumnWidth(10)
    private String beishaFirstLeg;

    @ExcelProperty("北沙大船")
    @ColumnWidth(10)
    private String beishaMotherVessel;

    @ExcelProperty("滘心OF")
    @ColumnWidth(10)
    private String ofJiaoxin;

    @ExcelProperty("滘心头程")
    @ColumnWidth(10)
    private String jiaoxinFirstLeg;

    @ExcelProperty("滘心大船")
    @ColumnWidth(10)
    private String jiaoxinMotherVessel;

    @ExcelProperty("时效")
    @ColumnWidth(10)
    private String transitTime;

    @ExcelProperty("船公司")
    @ColumnWidth(18)
    private String carrier;

    @ExcelProperty("有效期从")
    @ColumnWidth(14)
    private LocalDate validFrom;

    @ExcelProperty("有效期至")
    @ColumnWidth(14)
    private LocalDate validTo;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    private String remarks;
}
