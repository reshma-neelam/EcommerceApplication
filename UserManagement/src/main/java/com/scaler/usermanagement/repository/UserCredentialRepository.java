package com.scaler.usermanagement.repository;

import com.scaler.usermanagement.model.UserCredential;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
}
