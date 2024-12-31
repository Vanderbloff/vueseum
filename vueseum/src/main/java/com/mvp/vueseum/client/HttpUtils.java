package com.mvp.vueseum.client;

import com.mvp.vueseum.exception.AiProviderException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

public class HttpUtils {
    // Using builder pattern to create a reusable RestClient
    private static final RestClient restClient = RestClient.builder()
            .build();

    // Method for making POST requests that handles common JSON responses
    public static String postJsonRequest(String url, String body, Map<String, String> headers) {
        // Using RestClient's fluent API to build and execute the request
        return restClient.post()
                .uri(url)
                .headers(headerConsumer -> headers.forEach(headerConsumer::add))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    // Additional utility method for error handling and retries
    public static <T> T withRetry(Supplier<T> operation, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                if (attempts == maxRetries) {
                    throw new AiProviderException("Failed after " + maxRetries + " attempts", e);
                }
                // Exponential backoff
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiProviderException("Interrupted during retry", ie);
                }
            }
        }
        throw new AiProviderException("Failed to complete operation");
    }
}
