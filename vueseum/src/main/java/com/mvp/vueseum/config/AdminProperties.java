package com.mvp.vueseum.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "admin")
@Validated
public class AdminProperties {
    private String username;

    @NotBlank
    private String password;
}
