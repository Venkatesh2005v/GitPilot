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
}
