package com.example.gitpilot.architecture.release.dto;

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
public class ReleaseIntelligenceDto {
    private String releaseTag;
    private Long repositoryId;
    private LocalDateTime releaseDate;
    private String riskRating; // LOW, MEDIUM, HIGH, CRITICAL
    private int riskScore; // 0 - 100
    @Builder.Default
    private List<String> featuresAdded = new ArrayList<>();
    @Builder.Default
    private List<String> architecturalChanges = new ArrayList<>();
    @Builder.Default
    private List<String> technologyAdditions = new ArrayList<>();
    @Builder.Default
    private List<String> affectedModules = new ArrayList<>();
    private String summary;
    private String releaseKnowledgeNotes;
}
