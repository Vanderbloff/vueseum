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

    @PostConstruct
    public void logConfig() {
        log.info("Database URL being used: {}", dbUrl);
    }
}
