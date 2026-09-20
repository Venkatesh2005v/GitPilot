package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.HealthScoreDto;
import com.example.gitpilot.analysis.dto.ImprovementSuggestionDto;
import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.analysis.service.RepositoryEvidenceService.RepositoryEvidence;
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

    /**
     * Backward-compatible overload. Without concrete evidence, the deterministic fallback
     * cannot verify whether tests/CI already exist, so it is treated as unresolved.
     */
    public List<ImprovementSuggestionDto> generateSuggestions(Repository repository, TechStackDto techStack, HealthScoreDto healthScore, String readmeContent) {
        return generateSuggestions(repository, techStack, healthScore, readmeContent, RepositoryEvidence.unresolved());
    }

    public List<ImprovementSuggestionDto> generateSuggestions(Repository repository, TechStackDto techStack, HealthScoreDto healthScore, String readmeContent, RepositoryEvidence evidence) {
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
            return buildEvidenceBasedFallback(techStack, healthScore, readmeContent, evidence);
        }
    }

    /**
     * Deterministic, evidence-based fallback used when the AI gateway is unavailable.
     * It only surfaces GENUINE gaps derived from concrete repository evidence:
     *  - Recommends testing only if tests are NOT already present.
     *  - Recommends CI/CD only if no CI configuration is present.
     *  - Recommends Docker only if no Docker artifacts are present.
     *  - Recommends README/license improvements only when those are missing.
     * When evidence is unresolved (GitHub unavailable / no token), it does not assume a gap
     * for test/CI/Docker (avoids false "Add X" advice); it returns fewer, safe suggestions.
     * No randomization; identical inputs always yield identical output.
     */
    private List<ImprovementSuggestionDto> buildEvidenceBasedFallback(TechStackDto techStack,
                                                                      HealthScoreDto healthScore,
                                                                      String readmeContent,
                                                                      RepositoryEvidence evidence) {
        List<ImprovementSuggestionDto> suggestions = new ArrayList<>();
        List<String> manifests = techStack.getDetectedManifestFiles() != null ? techStack.getDetectedManifestFiles() : List.of();
        String manifestLower = String.join(" ", manifests).toLowerCase();

        boolean resolved = evidence != null && evidence.isResolved();
        boolean hasTests = evidence != null && evidence.isHasTests();
        boolean hasCI = evidence != null && evidence.isHasCI();
        boolean hasDocker = (evidence != null && evidence.isHasDocker())
                || manifestLower.contains("dockerfile") || manifestLower.contains("docker-compose");

        // Testing: only recommend if we CONFIRMED there are no tests.
        if (resolved && !hasTests) {
            String lang = techStack.getPrimaryLanguage() != null ? techStack.getPrimaryLanguage() : "";
            String testTooling = switch (lang) {
                case "Java", "Kotlin" -> "JUnit 5 and Mockito";
                case "JavaScript", "TypeScript" -> "Jest or Vitest";
                case "Python" -> "pytest";
                case "Go" -> "the built-in testing package";
                case "Rust" -> "the built-in test harness";
                default -> "your stack's standard test framework";
            };
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add automated tests")
                    .category("Testing")
                    .priority("HIGH")
                    .description("No test directory or test files were detected. Introduce automated tests using "
                            + testTooling + " to guard against regressions.")
                    .build());
        }

        // CI/CD: only recommend if we CONFIRMED there is no CI configuration.
        if (resolved && !hasCI) {
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Set up a CI pipeline")
                    .category("DevOps")
                    .priority("HIGH")
                    .description("No CI configuration (e.g. .github/workflows) was detected. Add a pipeline that builds and runs checks on every push.")
                    .build());
        }

        // Docker: only recommend if we CONFIRMED there are no Docker artifacts.
        if (resolved && !hasDocker) {
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add containerization")
                    .category("DevOps")
                    .priority("MEDIUM")
                    .description("No Dockerfile or docker-compose file was detected. Containerizing the app makes local setup and deployment reproducible.")
                    .build());
        }

        // Documentation: driven by health-score evidence (README presence is measured there).
        boolean hasReadme = readmeContent != null && readmeContent.trim().length() > 50;
        boolean noReadmeWeakness = healthScore != null && healthScore.getKeyWeaknesses() != null
                && healthScore.getKeyWeaknesses().stream().anyMatch(w -> w.toLowerCase().contains("readme"));
        if (!hasReadme || noReadmeWeakness) {
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Improve project documentation")
                    .category("Documentation")
                    .priority("MEDIUM")
                    .description("Add or expand the README with setup, usage, and architecture notes to speed up contributor onboarding.")
                    .build());
        }

        boolean noLicenseWeakness = healthScore != null && healthScore.getKeyWeaknesses() != null
                && healthScore.getKeyWeaknesses().stream().anyMatch(w -> w.toLowerCase().contains("license"));
        if (noLicenseWeakness) {
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Add a license file")
                    .category("Documentation")
                    .priority("LOW")
                    .description("No license file was detected. Adding one clarifies how others may use and contribute to the project.")
                    .build());
        }

        // If evidence could not be resolved and nothing else surfaced, provide a single honest,
        // non-generic note rather than fabricating gaps.
        if (suggestions.isEmpty()) {
            suggestions.add(ImprovementSuggestionDto.builder()
                    .title("Re-run AI analysis for tailored recommendations")
                    .category("Analysis")
                    .priority("LOW")
                    .description("Detailed recommendations are generated by the AI analyzer. It was unavailable and repository evidence was insufficient to identify concrete gaps. Use the refresh button to retry.")
                    .build());
        }

        return suggestions;
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
