package com.example.gitpilot.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GroqProvider implements AIProvider {

    private final RestClient restClient;

    @Value("${ai.groq.api-key:}")
    private String apiKey;

    public GroqProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "Groq";
    }

    @Override
    public String getModelName() {
        return "llama-3.3-70b-versatile";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String generateInsight(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Groq API key is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
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
