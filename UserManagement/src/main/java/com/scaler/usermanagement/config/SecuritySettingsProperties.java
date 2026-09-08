package com.scaler.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecuritySettingsProperties {

    private Refresh refresh = new Refresh();

    @Getter
    @Setter
    public static class Refresh {
        private long ttlDays;
    }
}
