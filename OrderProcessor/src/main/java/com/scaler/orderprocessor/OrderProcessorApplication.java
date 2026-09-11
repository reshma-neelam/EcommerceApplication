package com.scaler.orderprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OrderProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderProcessorApplication.class, args);
    }
}
