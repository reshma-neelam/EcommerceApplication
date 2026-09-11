package com.scaler.orderprocessor.controller;

import com.scaler.orderprocessor.enums.OutboxStatus;
import com.scaler.orderprocessor.repository.OutboxEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/outbox")
@RequiredArgsConstructor
public class InternalOutboxController {

    private final OutboxEventRepository outboxEventRepository;

    @PostMapping("/{id}/requeue")
    @Transactional
    public ResponseEntity<Void> requeue(@PathVariable UUID id) {
        return outboxEventRepository.findById(id)
                .map(row -> {
                    row.setStatus(OutboxStatus.NEW);
                    row.setRetryCount(0);
                    row.setLastError(null);
                    outboxEventRepository.save(row);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
