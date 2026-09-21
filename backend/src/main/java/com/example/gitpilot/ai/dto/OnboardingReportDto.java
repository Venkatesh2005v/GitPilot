package com.example.gitpilot.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingReportDto {
    private Long repositoryId;
    private String repositoryName;
    private String projectPurpose;
    private String highLevelDescription;
    @Builder.Default
    private List<String> technologyStack = new ArrayList<>();
    @Builder.Default
    private List<ModuleDescriptionDto> mainModules = new ArrayList<>();
    @Builder.Default
    private List<String> recommendedLearningPath = new ArrayList<>();
    @Builder.Default
    private List<String> importantConfigFiles = new ArrayList<>();
    @Builder.Default
    private List<String> keyRestApis = new ArrayList<>();
    @Builder.Default
    private List<String> suggestedFirstContributions = new ArrayList<>();
    private LocalDateTime generatedAt;
    private boolean isCached;

    // ---- Phase 3: Project Foundation (additive; optional; backward compatible) ----
    /** What problem the project solves, derived from evidence. May state that evidence is insufficient. */
    private String problemSolved;
    /** Major capabilities/requirements supported by repository evidence. */
    @Builder.Default
    private List<String> primaryRequirements = new ArrayList<>();
    /** Features considered IMPLEMENTED because they are backed by concrete code/structure/config evidence. */
    @Builder.Default
    private List<ImplementedFeatureDto> implementedFeatures = new ArrayList<>();

    // ---- Phase 3 expansion: structured Project Foundation (all additive/optional) ----
    /** Deterministic project overview (tech/db/integrations/infra) built from the fingerprint. */
    private ProjectOverviewDto projectOverview;
    /** Capabilities with an explicit status: IMPLEMENTED | PARTIAL | NOT_DETERMINED. */
    @Builder.Default
    private List<CapabilityDto> capabilities = new ArrayList<>();
    /** Concise high-level flow derived from the repository, e.g. "Frontend -> Controller -> Service -> Database". */
    @Builder.Default
    private List<String> systemFlow = new ArrayList<>();
    /** Configuration/environment categories required to run the project (no secret values). */
    @Builder.Default
    private List<String> configEnvironment = new ArrayList<>();
    /** Local setup / getting-started steps derived from actual build/Docker/config evidence. */
    @Builder.Default
    private List<String> gettingStartedSteps = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleDescriptionDto {
        private String moduleName;
        private String responsibility;
        private List<String> keyFiles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImplementedFeatureDto {
        private String name;
        private String description;
        /** Why this is considered implemented (e.g. "controller class present", "dependency detected"). */
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectOverviewDto {
        private String primaryLanguage;
        private String backend;      // framework + build tool, e.g. "Spring Boot (Maven)"
        private String frontend;     // framework + build tool, e.g. "React (Vite)"
        private String database;     // e.g. "PostgreSQL" or null
        @Builder.Default
        private List<String> integrations = new ArrayList<>();   // evidence-based
        @Builder.Default
        private List<String> infrastructure = new ArrayList<>(); // Docker, Compose, CI
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapabilityDto {
        private String name;
        private String description;
        /** IMPLEMENTED | PARTIAL | NOT_DETERMINED */
        private String status;
        private String evidence;
    }
}
