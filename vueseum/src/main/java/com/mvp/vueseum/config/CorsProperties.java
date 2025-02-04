package com.mvp.vueseum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@ConfigurationProperties(prefix = "management.endpoints.web")
@Configuration
public class CorsProperties {
    private Cors cors = new Cors(null, null, null, 0);

    public record Cors(
            String[] allowedOrigins,
            String[] allowedMethods,
            String[] allowedHeaders,
            long maxAge
    ) {}
}