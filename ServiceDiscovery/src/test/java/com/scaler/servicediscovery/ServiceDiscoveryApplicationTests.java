package com.scaler.servicediscovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"server.port=0", "eureka.client.register-with-eureka=false", "eureka.client.fetch-registry=false"})
class ServiceDiscoveryApplicationTests {

    @Test
    void contextLoads() {}
}
