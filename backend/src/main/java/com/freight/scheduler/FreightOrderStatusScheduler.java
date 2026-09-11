package com.freight.scheduler;

import com.freight.service.FreightOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 定期根据 ETA 推进订单状态，确保服务重启后也能补齐已到港订单。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreightOrderStatusScheduler {

    private final FreightOrderService orderService;

    @Scheduled(initialDelay = 0, fixedDelay = 60 * 60 * 1000L)
    public void refreshOrderStatuses() {
        int updated = orderService.updateStatusesByEta(LocalDate.now());
        if (updated > 0) {
            log.info("根据 ETA 自动更新订单状态：{} 条", updated);
        }
    }
}
