package com.mvp.vueseum.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class KeyVaultValidator {
    @PostConstruct
    void validateKeyVault() {
        try {
            // Try direct managed identity access, bypassing Spring's configuration
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            SecretClient client = new SecretClientBuilder()
                    .vaultUrl("https://vueseum-kv-prod.vault.azure.net")
                    .credential(credential)
                    .buildClient();

            // Try to get any secret
            log.info("Attempting direct Key Vault access...");
            client.listPropertiesOfSecrets().stream().findFirst().ifPresent(
                    secretProperties -> log.info("Found secret with name: {}",
                            secretProperties.getName())
            );
        } catch (Exception e) {
            log.error("Failed to access Key Vault directly: ", e);
        }
    }
}
