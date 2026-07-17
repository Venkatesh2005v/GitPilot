package com.example.gitpilot.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements AIProvider {

    private final RestClient restClient;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    public GeminiProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public String getModelName() {
        return "gemini-2.5-flash";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String generateInsight(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractTextFromGemini(response);
    }

    private String extractTextFromGemini(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
