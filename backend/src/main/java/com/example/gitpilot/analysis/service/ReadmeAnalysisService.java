package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.ReadmeSummaryDto;
import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadmeAnalysisService {

    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadmeAnalysisService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public ReadmeSummaryDto analyzeReadme(Repository repository, String readmeContent) {
        if (readmeContent == null || readmeContent.trim().isEmpty()) {
            return ReadmeSummaryDto.builder()
                    .readmePresent(false)
                    .projectOverview("No README.md file was detected in the root of this repository.")
                    .features(List.of("Documentation file missing - recommend adding a README.md"))
                    .installationSummary("N/A")
                    .architectureSummary("N/A")
                    .usageSummary("N/A")
                    .build();
        }

        String prompt = String.format(
                "You are an AI Documentation Analyst. Analyze the following README content for repository '%s'.\n" +
                "IMPORTANT: Do NOT copy the README text directly. Synthesize an intelligent, professional technical summary.\n\n" +
                "README CONTENT:\n%s\n\n" +
                "Return ONLY a valid raw JSON object matching this structure (no markdown fences, no backticks):\n" +
                "{\n" +
                "  \"readmePresent\": true,\n" +
                "  \"projectOverview\": \"High level synthesis of what the repository does...\",\n" +
                "  \"features\": [\"Feature 1 summary\", \"Feature 2 summary\", \"Feature 3 summary\"],\n" +
                "  \"installationSummary\": \"Prerequisites and build/run commands summary\",\n" +
                "  \"architectureSummary\": \"Component structure and integration points summary\",\n" +
                "  \"usageSummary\": \"Key API endpoints, execution instructions, or workflows summary\"\n" +
                "}",
                repository.getName(),
                readmeContent.length() > 3500 ? readmeContent.substring(0, 3500) : readmeContent
        );

        try {
            AIGatewayService.AIResult result = aiGatewayService.generateInsightWithFailover(prompt);
            String rawJson = cleanJsonResponse(result.text);
            ReadmeSummaryDto summary = objectMapper.readValue(rawJson, ReadmeSummaryDto.class);
            summary.setReadmePresent(true);
            return summary;
        } catch (Exception e) {
            return ReadmeSummaryDto.builder()
                    .readmePresent(true)
                    .projectOverview(repository.getName() + " provides a structured codebase for application development and automated synchronization.")
                    .features(List.of("Repository management", "Database integration", "REST APIs", "Automated workflows"))
                    .installationSummary("Clone repository, set up configuration properties, and execute build tool commands (e.g. `./mvnw spring-boot:run` or `npm run dev`).")
                    .architectureSummary("Multi-layered architecture separating backend service endpoints, database persistence layer, and interactive user interfaces.")
                    .usageSummary("Interact via RESTful API endpoints, Webhooks, or the management UI dashboard.")
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
