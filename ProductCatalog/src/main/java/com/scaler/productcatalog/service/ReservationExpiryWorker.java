package com.scaler.productcatalog.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically expires due inventory reservations and returns their held stock. */
@Component
@RequiredArgsConstructor
public class ReservationExpiryWorker {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryWorker.class);

    private final InventoryService inventoryService;

    @Scheduled(fixedDelayString = "${productcatalog.reservation.expiry-scan-ms:60000}")
    public void expireDueReservations() {
        int expired = inventoryService.expireDue(Instant.now());
        if (expired > 0) {
            log.info("Expired {} inventory reservation(s)", expired);
        }
    }
}
