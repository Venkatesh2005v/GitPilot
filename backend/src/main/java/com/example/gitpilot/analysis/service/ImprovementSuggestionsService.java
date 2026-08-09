package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.HealthScoreDto;
import com.example.gitpilot.analysis.dto.ImprovementSuggestionDto;
import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImprovementSuggestionsService {

    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImprovementSuggestionsService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public List<ImprovementSuggestionDto> generateSuggestions(Repository repository, TechStackDto techStack, HealthScoreDto healthScore, String readmeContent) {
        String readmeSnippet = (readmeContent != null && readmeContent.length() > 500) ? readmeContent.substring(0, 500) : (readmeContent != null ? readmeContent : "");
        String prompt = String.format(
                "You are a Software Engineering Advisor analyzing repository '%s'.\n" +
                "Detected Technologies: %s\n" +
                "Primary Language: %s\n" +
                "Manifest Files: %s\n" +
                "Health Score: %d/100 (Grade: %s)\n" +
                "Documentation Score: %d/100\n" +
                "Activity Score: %d/100\n" +
                "README excerpt: %s\n\n" +
                "Based on this SPECIFIC repository's profile, generate 3-5 practical, actionable improvement suggestions.\n" +
                "Each suggestion must be relevant to the detected technologies and current health gaps.\n" +
                "Return ONLY a valid raw JSON array (no markdown fences):\n" +
                "[\n" +
                "  {\"title\": \"...\", \"category\": \"...\", \"priority\": \"HIGH|MEDIUM|LOW\", \"description\": \"...\"}\n" +
                "]",
                repository.getName(),
                techStack.getDetectedTechnologies(),
                techStack.getPrimaryLanguage(),
                techStack.getDetectedManifestFiles(),
                healthScore.getOverallHealthScore(),
                healthScore.getGrade() != null ? healthScore.getGrade() : "Unknown",
                healthScore.getDocumentationScore() != null ? healthScore.getDocumentationScore() : 0,
                healthScore.getActivityScore() != null ? healthScore.getActivityScore() : 0,
                readmeSnippet.isEmpty() ? "(No README)" : readmeSnippet
        );

        try {
            AIGatewayService.AIResult result = aiGatewayService.generateInsightWithFailover(prompt);
            String rawJson = cleanJsonResponse(result.text);
            return objectMapper.readValue(rawJson, new TypeReference<List<ImprovementSuggestionDto>>() {});
        } catch (Exception e) {
            List<ImprovementSuggestionDto> defaultSuggestions = new ArrayList<>();

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add Unit Testing")
                    .category("Testing")
                    .priority("HIGH")
                    .description("Implement comprehensive unit tests with JUnit 5 and Mockito for domain services.")
                    .build());

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add Integration Testing")
                    .category("Testing")
                    .priority("MEDIUM")
                    .description("Configure Testcontainers or mock web environments for automated API integration testing.")
                    .build());

            if (!techStack.getDetectedManifestFiles().contains("Dockerfile") && !techStack.getDetectedManifestFiles().contains("docker-compose.yml")) {
                defaultSuggestions.add(ImprovementSuggestionDto.builder()
                        .title("Add Docker Compose")
                        .category("DevOps")
                        .priority("HIGH")
                        .description("Provide a `docker-compose.yml` to spin up PostgreSQL and application dependencies effortlessly.")
                        .build());
            }

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Configure CI/CD")
                    .category("DevOps")
                    .priority("HIGH")
                    .description("Add a `.github/workflows/ci.yml` pipeline for continuous integration and automated build verification.")
                    .build());

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Improve Logging")
                    .category("Quality")
                    .priority("MEDIUM")
                    .description("Introduce structured SLF4J/MDC contextual logging for enhanced production observability.")
                    .build());

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add Caching")
                    .category("Performance")
                    .priority("MEDIUM")
                    .description("Implement Spring Cache or Redis caching for frequent repository read queries.")
                    .build());

            defaultSuggestions.add(ImprovementSuggestionDto.builder()
                    .title("Improve Exception Handling")
                    .category("Architecture")
                    .priority("LOW")
                    .description("Ensure all API exceptions yield standard RFC 7807 Problem Details response formats.")
                    .build());

            return defaultSuggestions;
        }
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "[]";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```")) trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        return trimmed.trim();
    }
}
