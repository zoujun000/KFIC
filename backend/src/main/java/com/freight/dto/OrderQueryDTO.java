package com.freight.dto;

import lombok.Data;

@Data
public class OrderQueryDTO {
    private String orderSo;
    private String orderNo;
    private Long customerId;
    private String shipType;
    private String status;
    /** 由后端自动注入，前端无需传 */
    private Long createdBy;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
