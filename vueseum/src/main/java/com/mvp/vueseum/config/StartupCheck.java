package com.mvp.vueseum.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupCheck {
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username:not-found}")
    private String dbUsername;

    @Value("${prod-db-url:not-found}")
    private String rawDbUrl;

    @Value("${management.endpoints.web.cors.allowed-headers:not-found}")
    private String corsHeaders;

    @PostConstruct
    public void logConfig() {
        log.info("Raw DB URL from Key Vault: {}", rawDbUrl);
        log.info("Resolved Database URL: {}", dbUrl);
        log.info("Database Username: {}", dbUsername);
        log.info("CORS Headers (known working): {}", corsHeaders);
    }
}
