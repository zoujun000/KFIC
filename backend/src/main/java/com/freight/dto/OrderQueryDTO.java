package com.freight.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OrderQueryDTO {
    private String orderSo;
    private String orderNo;
    private Long customerId;
    private String shipType;
    private String status;
    /** 多选状态，逗号分隔 */
    private String statuses;
    /** 由后端自动注入，前端无需传 */
    private Long createdBy;
    /** ETD 起始日期 */
    private LocalDate etdStart;
    /** ETD 结束日期 */
    private LocalDate etdEnd;
    /** 订单创建时间起始日期（包含当天） */
    private LocalDate createTimeStart;
    /** 订单创建时间结束日期（包含当天） */
    private LocalDate createTimeEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
