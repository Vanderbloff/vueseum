package com.mvp.vueseum.config;

import com.azure.spring.cloud.autoconfigure.implementation.keyvault.environment.KeyVaultEnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevConfiguration {
    @Bean
    public KeyVaultEnvironmentPostProcessor keyVaultEnvironmentPostProcessor() {
        return null;  // Effectively disables the Key Vault post processor for dev profile
    }
}
