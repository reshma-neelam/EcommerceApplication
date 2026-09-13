package com.scaler.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "notification.kafka.consumer-enabled=false",
        "notification.mail.enabled=false"
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
