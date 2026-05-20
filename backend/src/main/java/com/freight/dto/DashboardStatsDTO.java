package com.freight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalCustomers;
    private long totalOrders;
    private long inProgressOrders;
    private long completedOrders;
    private List<FreightOrderDTO> recentOrders;
}
