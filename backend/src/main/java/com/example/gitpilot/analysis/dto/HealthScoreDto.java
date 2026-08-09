package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HealthScoreDto {
    private Integer overallHealthScore; // 0-100
    private String grade;              // Excellent, Good, Fair, Needs Attention
    private Integer documentationScore;
    private Integer activityScore;
    private Integer collaborationScore;
    private Integer structureScore;
    // Legacy fields for backward compat
    private String documentationRating;
    private String testingRating;
    private String architectureRating;
    private String maintainabilityRating;
    private List<String> keyStrengths;
    private List<String> keyWeaknesses;
    private String aiExplanation;
}
