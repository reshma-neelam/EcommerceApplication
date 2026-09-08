package com.scaler.usermanagement.repository;

import com.scaler.usermanagement.model.UserAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserId(UUID userId);

    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);
}
