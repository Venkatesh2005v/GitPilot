package com.example.gitpilot.architecture.drift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftViolationDto {
    private String violationType; // CONTROLLER_DIRECT_REPOSITORY_ACCESS, CIRCULAR_DEPENDENCY, PACKAGE_BOUNDARY_VIOLATION, GOD_CLASS, IMPROPER_LAYERING
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String sourceComponent;
    private String targetComponent;
    private String ruleName;
    private String description;
    private String recommendation;
    private String aiExplanation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriftReportSummaryDto {
        private Long repositoryId;
        private int totalViolations;
        private int criticalViolations;
        private int highViolations;
        private int mediumViolations;
        private int architecturalHealthScore; // 0 - 100
        @Builder.Default
        private List<DriftViolationDto> violations = new ArrayList<>();
    }
}
