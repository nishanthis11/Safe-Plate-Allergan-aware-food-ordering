package com.example.smartfoodordering.scheduler;

import com.example.smartfoodordering.service.OrderService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancels orders the restaurant never confirmed, refunding any captured payment. */
@Component
public class OrderCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);
    private static final Duration STALE_AFTER = Duration.ofMinutes(30);

    private final OrderService orderService;

    public OrderCleanupScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void cancelStaleOrders() {
        int cancelled = orderService.cancelStalePendingOrders(STALE_AFTER);
        if (cancelled > 0) {
            log.info("Auto-cancelled {} stale pending order(s)", cancelled);
        }
    }
}
