package com.scaler.orderprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.orderprocessor.dto.ApiErrorDTO;
import com.scaler.orderprocessor.observability.CorrelationId;
import com.scaler.orderprocessor.security.JwtAuthenticationFilter;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, ProductCatalogProperties.class, OrderPricingProperties.class})
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/orders/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiErrorDTO body = ApiErrorDTO.builder()
                        .code("UNAUTHORIZED")
                        .message("Authentication required")
                        .correlationId(CorrelationId.current())
                        .timestamp(Instant.now())
                        .build();
                objectMapper.writeValue(response.getWriter(), body);
            }));
        return http.build();
    }
}
