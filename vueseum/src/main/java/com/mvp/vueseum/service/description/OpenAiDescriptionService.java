package com.mvp.vueseum.service.description;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.vueseum.exception.RetryException;
import com.mvp.vueseum.exception.AiProviderException;
import com.mvp.vueseum.service.BaseDescriptionService;
import com.mvp.vueseum.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;

@Service
@Primary
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiDescriptionService extends BaseDescriptionService {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final RetryUtil retryUtil;

    public OpenAiDescriptionService(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.url}") String apiUrl,
            @Value("${ai.openai.model}") String model,
            ObjectMapper objectMapper,
            RetryUtil retryUtil) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.retryUtil = retryUtil;
    }

    @Override
    protected String generateDescription(String prompt) {
        // Log prompt size to help diagnose timeout issues
        log.info("Generating description - prompt length: {} characters", prompt.length());

        // Add warning if prompt is very large
        if (prompt.length() > 4000) {
            log.warn("Very large prompt detected ({} chars) - may cause timeout issues",
                    prompt.length());
        }

        try {
            return retryUtil.withRetry(
                    () -> makeOpenAiRequest(prompt),
                    "OpenAI description generation",
                    3
            );
        } catch (RetryException e) {
            throw new AiProviderException("Failed to generate description after retries", e);
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

        try {
            Map<String, Object> requestPayload = getStringObjectMap(prompt);
            String requestBody = objectMapper.writeValueAsString(requestPayload);

            log.debug("OpenAI request payload: {}", requestBody);

            String response = RestClient.builder()
                    .baseUrl(apiUrl)
                    .build()
                    .post()
                    .headers(headerConsumer -> headers.forEach(headerConsumer::add))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractContentFromResponse(response);
        } catch (Exception e) {
            if (e.getCause() instanceof SocketTimeoutException || e.getMessage().contains("timeout") || e.getMessage().contains("Read timed out")) {

                log.error("OpenAI request timed out: {}", e.getMessage());
                log.error("This usually indicates network latency issues or an overloaded API endpoint");
                log.error("Consider increasing the timeout or reducing prompt size");
            } else {
                log.error("OpenAI request failed: {} ({})", e.getMessage(), e.getClass().getName());
            }

            throw new AiProviderException("OpenAI request failed: " + e.getMessage(), e);
        }
    }

    private @NotNull Map<String, Object> getStringObjectMap(String prompt) {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", model);
        requestPayload.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();

        // System message
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an expert museum curator and art historian.");
        messages.add(systemMessage);

        // User message with the prompt
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestPayload.put("messages", messages);
        return requestPayload;
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
