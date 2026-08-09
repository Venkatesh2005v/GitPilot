package com.example.gitpilot.architecture.techdebt.dto;

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
public class CodeMetricsDto {
    private String className;
    private String nodeKey;
    private String category;
    private int loc;
    private int cyclomaticComplexity;
    private int fanIn;
    private int fanOut;
    private double instabilityIndex; // 0.0 (completely stable) to 1.0 (completely instable)
    private int inheritanceDepth;
    private int techDebtScore; // 0 - 100
    private String refactoringRecommendation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryTechDebtOverviewDto {
        private Long repositoryId;
        private int averageComplexity;
        private double averageInstability;
        private int highestTechDebtScore;
        private String highestDebtClass;
        @Builder.Default
        private List<CodeMetricsDto> classMetrics = new ArrayList<>();
    }
}
