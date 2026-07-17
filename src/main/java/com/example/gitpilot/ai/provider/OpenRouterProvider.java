package com.example.gitpilot.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenRouterProvider implements AIProvider {

    private final RestClient restClient;

    @Value("${ai.openrouter.api-key:}")
    private String apiKey;

    public OpenRouterProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "OpenRouter";
    }

    @Override
    public String getModelName() {
        return "google/gemini-2.5-flash";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String generateInsight(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenRouter API key is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "model", "google/gemini-2.5-flash",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractTextFromOpenAiCompatible(response);
    }

    private String extractTextFromOpenAiCompatible(Map<?, ?> response) {
        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse provider response: " + e.getMessage(), e);
        }
    }
}
