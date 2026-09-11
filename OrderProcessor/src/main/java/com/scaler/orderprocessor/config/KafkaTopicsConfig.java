package com.scaler.orderprocessor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "order.kafka.topics-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfig {

    @Bean
    public NewTopic orderEventsTopic(
            @Value("${order.kafka.order-events-topic:order-events}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
