package com.scaler.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NotificationMessage {
    private final String recipient;
    private final String subject;
    private final String body;
}
