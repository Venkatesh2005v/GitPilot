package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryIntelligenceResponse {
    private Long repositoryId;
    private String repositoryName;
    private RepositorySummaryDto summary;
    private TechStackDto techStack;
    private ReadmeSummaryDto readmeSummary;
    private CommitSummaryDto commitSummary;
    private List<ImprovementSuggestionDto> suggestions;
    private HealthScoreDto healthScore;
    private String providerUsed;
    private String modelUsed;
    private String fallbackUsed;
    private LocalDateTime generatedTime;
    private Boolean cached;
    // New dynamic fields
    private Integer analysisConfidence; // 0-100 percentage
    private String grade;               // Excellent, Good, Fair, Needs Attention
    private String aiExplanation;       // AI-generated explanation of the score
}
