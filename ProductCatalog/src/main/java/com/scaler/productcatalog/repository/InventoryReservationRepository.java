package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.enums.ReservationStatus;
import com.scaler.productcatalog.model.InventoryReservation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    List<InventoryReservation> findByOrderId(UUID orderId);

    List<InventoryReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
