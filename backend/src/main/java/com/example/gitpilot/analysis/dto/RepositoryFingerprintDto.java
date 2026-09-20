package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

/**
 * Deterministic, structured technology fingerprint derived from actual repository files.
 * Every entry carries the concrete evidence it was derived from, so downstream consumers
 * (Repository Intelligence / AI Recommendations / Onboarding) can distinguish strong
 * dependency/config evidence from weak README mentions.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryFingerprintDto {

    /** True once at least the repository root listing was read successfully. */
    private boolean resolved;

    // Languages
    private String primaryLanguage;
    @Singular("language")
    private List<String> languages;

    // Backend
    private String backendFramework;      // e.g. "Spring Boot", "Express", "Django", "FastAPI", "Flask", "NestJS"
    private String backendBuildTool;      // e.g. "Maven", "Gradle", "npm", "pip", "Poetry"
    @Singular("backendLibrary")
    private List<String> backendLibraries;

    // Frontend
    private String frontendFramework;     // e.g. "React", "Vue", "Angular", "Next.js"
    private String frontendBuildTool;     // e.g. "Vite", "webpack"
    @Singular("frontendLibrary")
    private List<String> frontendLibraries;

    // Database
    private String database;              // e.g. "PostgreSQL", "MySQL", "MongoDB"

    // Testing
    @Singular("testingFramework")
    private List<String> testingFrameworks;

    // Infrastructure
    private boolean docker;
    private boolean dockerCompose;
    private String ciProvider;            // e.g. "GitHub Actions", "GitLab CI", "Jenkins", "CircleCI"

    // Evidence: human-readable "Technology — evidence" lines, e.g. "Spring Boot — pom.xml dependency".
    @Singular("evidence")
    private List<TechnologyEvidenceDto> evidence;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TechnologyEvidenceDto {
        private String technology;   // e.g. "Spring Boot"
        private String category;     // LANGUAGE, BACKEND, FRONTEND, DATABASE, TESTING, INFRA
        private String source;       // e.g. "pom.xml dependency", ".github/workflows/*.yml"
        private String strength;     // STRONG (dependency/config), MEDIUM (file structure), WEAK (README)
    }
}
