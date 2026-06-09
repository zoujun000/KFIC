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
    /** 由后端自动注入，前端无需传 */
    private Long createdBy;
    /** ETD 起始日期 */
    private LocalDate etdStart;
    /** ETD 结束日期 */
    private LocalDate etdEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
