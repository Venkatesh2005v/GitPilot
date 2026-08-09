package com.example.gitpilot.architecture.impact.dto;

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
public class ImpactReportDto {
    private Long repositoryId;
    private String commitHash;
    private String author;
    private LocalDateTime timestamp;
    private int blastRadiusScore; // 0 to 100
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    @Builder.Default
    private List<String> directlyModifiedClasses = new ArrayList<>();
    @Builder.Default
    private List<String> affectedDownstreamClasses = new ArrayList<>();
    @Builder.Default
    private List<String> affectedApis = new ArrayList<>();
    @Builder.Default
    private List<String> affectedServices = new ArrayList<>();
    private String summary;
    private String aiExplanation;
}
