package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.RepositorySummaryDto;
import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RepositoryAnalysisService {

    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RepositoryAnalysisService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public RepositorySummaryDto analyzeRepository(Repository repository, TechStackDto techStack, List<String> commitMessages, String readmeText) {
        String prompt = String.format(
                "You are an AI Repository Intelligence Architect. Analyze this repository metadata:\n" +
                "Repository Name: %s\n" +
                "Default Branch: %s\n" +
                "Detected Tech Stack: %s\n" +
                "Last Synced: %s\n" +
                "Recent Commit Messages: %s\n" +
                "README Sample: %s\n\n" +
                "Generate a concise, highly accurate repository summary JSON matching this EXACT structure (no markdown fences, raw JSON only):\n" +
                "{\n" +
                "  \"projectPurpose\": \"Clear 1-2 sentence description of what this project accomplishes.\",\n" +
                "  \"primaryTechnologies\": [\"Java\", \"TypeScript\"],\n" +
                "  \"frameworksDetected\": [\"Spring Boot 4\", \"React\", \"Flyway\"],\n" +
                "  \"architectureStyle\": \"Layered REST Microservice / Full-Stack Monolith\",\n" +
                "  \"mainFunctionality\": \"Centralized repository intelligence tracking, OAuth2 auth, webhooks, AI analysis\",\n" +
                "  \"complexityLevel\": \"Medium\",\n" +
                "  \"lastUpdatedInfo\": \"Recently updated with active commits and webhook event listeners.\"\n" +
                "}",
                repository.getName(),
                repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main",
                techStack.getDetectedTechnologies(),
                repository.getLastSyncedAt() != null ? repository.getLastSyncedAt().toString() : LocalDateTime.now().toString(),
                commitMessages.stream().limit(10).toList(),
                readmeText != null && readmeText.length() > 500 ? readmeText.substring(0, 500) : (readmeText != null ? readmeText : "None")
        );

        try {
            AIGatewayService.AIResult result = aiGatewayService.generateInsightWithFailover(prompt);
            String rawJson = cleanJsonResponse(result.text);
            return objectMapper.readValue(rawJson, RepositorySummaryDto.class);
        } catch (Exception e) {
            // Fallback deterministic summary if AI provider offline
            return RepositorySummaryDto.builder()
                    .projectPurpose("AI analysis unavailable. Unable to generate summary for " + repository.getName() + ".")
                    .primaryTechnologies(techStack.getDetectedTechnologies().stream().limit(3).toList())
                    .frameworksDetected(techStack.getDetectedTechnologies())
                    .architectureStyle("Unknown - AI analysis required")
                    .mainFunctionality("Unable to determine. Retry AI analysis.")
                    .complexityLevel("Unknown")
                    .lastUpdatedInfo("Last synced: " + (repository.getLastSyncedAt() != null ? repository.getLastSyncedAt().toString() : "Unknown"))
                    .build();
        }
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```")) trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        return trimmed.trim();
    }
}
