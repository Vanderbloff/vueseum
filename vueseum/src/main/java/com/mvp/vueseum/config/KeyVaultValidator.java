package com.mvp.vueseum.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeyVaultValidator {
    private final Environment environment;

    public KeyVaultValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateKeyVault() {
        if (isKeyVaultDisabled()) {
            log.info("Skipping Key Vault validation in development mode.");
            return;
        }

        try {
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            SecretClient client = new SecretClientBuilder()
                    .vaultUrl("https://vueseum-kv-prod.vault.azure.net")
                    .credential(credential)
                    .buildClient();

            log.info("Attempting direct Key Vault access...");
            client.listPropertiesOfSecrets().stream().findFirst().ifPresent(
                    secretProperties -> log.info("Found secret with name: {}", secretProperties.getName())
            );
        } catch (Exception e) {
            log.error("Failed to access Key Vault directly: ", e);
        }
    }

    private boolean isKeyVaultDisabled() {
        return environment.matchesProfiles("dev");
    }
}