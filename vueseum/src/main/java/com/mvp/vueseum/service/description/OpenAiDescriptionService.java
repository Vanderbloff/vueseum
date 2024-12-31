package com.mvp.vueseum.service.description;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.vueseum.client.HttpUtils;
import com.mvp.vueseum.exception.AiProviderAuthException;
import com.mvp.vueseum.exception.AiProviderException;
import com.mvp.vueseum.exception.AiProviderRateLimitException;
import com.mvp.vueseum.service.BaseDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Primary
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiDescriptionService extends BaseDescriptionService {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiDescriptionService(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.url}") String apiUrl,
            @Value("${ai.openai.model}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    protected String generateDescription(String prompt) {
        try {
            return HttpUtils.withRetry(() -> makeOpenAiRequest(prompt), 3);
        } catch (HttpClientErrorException e) {
            switch (e.getStatusCode()) {
                case UNAUTHORIZED:
                    throw new AiProviderAuthException("Invalid API key");
                case TOO_MANY_REQUESTS:
                    Duration retryAfter = parseRetryAfter(Objects.requireNonNull(e.getResponseHeaders()));
                    throw new AiProviderRateLimitException("Too many requests", retryAfter);
                default:
                    throw new AiProviderException("OpenAI request failed: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new AiProviderException("Failed to generate description: " + e.getMessage(), e);
        }
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(retryAfter));
            } catch (NumberFormatException e) {
                log.warn("Invalid Retry-After header: {}", retryAfter);
            }
        }
        return Duration.ofMinutes(1); // Default retry after
    }

    private String makeOpenAiRequest(String prompt) {
        // Create headers with API key
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + apiKey
        );

        // Construct the request body according to OpenAI's API format
        String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are an expert museum curator and art historian."
                    },
                    {
                        "role": "user",
                        "content": "%s"
                    }
                ],
                "temperature": 0.7
            }
            """.formatted(model, prompt.replace("\"", "\\\""));

        String response = HttpUtils.postJsonRequest(apiUrl, requestBody, headers);
        return extractContentFromResponse(response);
    }

    private String extractContentFromResponse(String response) {
        try {
            JsonNode rootNode = this.objectMapper.readTree(response);
            JsonNode choicesNode = rootNode.path("choices");

            // First validate we have any choices at all
            if (choicesNode.isEmpty() || !choicesNode.isArray()) {
                throw new AiProviderException("No response choices provided by AI service");
            }

            // Get the first choice
            JsonNode firstChoice = choicesNode.get(0);
            String finishReason = firstChoice.path("finish_reason").asText();

            // Get the content
            String content = firstChoice.path("message")
                    .path("content")
                    .asText("")  // Default to empty string if missing
                    .trim();

            // If content is empty, that's a problem
            if (content.isEmpty()) {
                throw new AiProviderException("AI service returned empty content");
            }

            // Handle different finish reasons
            return switch (finishReason) {
                case "stop" -> content; // Return the full content
                case "length" -> content + " [Note: This description may be incomplete.]"; // Content was cut off - append a note
                case "content_filter" -> getFallbackDescription(); // Content was filtered - return a standard message
                default -> throw new AiProviderException(
                        "Unexpected response termination: " + finishReason
                );
            };

        } catch (JsonProcessingException e) {
            throw new AiProviderException("Failed to parse AI provider response", e);
        }
    }

    private String getFallbackDescription() {
        return "A detailed description of this artwork is currently unavailable. " +
                "Please refer to the provided artwork details below for more information.";
    }
}
