package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.dto.FreightOrderDTO;
import com.freight.dto.OrderQueryDTO;
import com.freight.entity.FreightOrder;

public interface FreightOrderService {
    IPage<FreightOrder> page(OrderQueryDTO query);
    FreightOrder getById(Long id);
    void create(FreightOrderDTO dto);
    void update(FreightOrderDTO dto);
    void updateStatus(Long id, String status);
    void delete(Long id);
}
