package com.example.gitpilot.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    private Long id;
    private Long repositoryId;
    private String title;
    private String category;
    private String priority;
    private String reason;
    private String estimatedEffort;
    private String expectedImpact;
    private String targetModule;
    private Boolean actionTaken;
}
