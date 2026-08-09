package com.example.gitpilot.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDNADto {
    private Long id;
    private Long repositoryId;
    private Integer activityLevel;
    private Integer repositorySize;
    private Integer architectureQuality;
    private Integer testingStrength;
    private Integer documentationQuality;
    private Integer deploymentReadiness;
    private Integer riskLevel;
    private Integer maintainability;
    private Integer knowledgeScore;
    private String personalityArchetype;
    private LocalDateTime updatedAt;
}
