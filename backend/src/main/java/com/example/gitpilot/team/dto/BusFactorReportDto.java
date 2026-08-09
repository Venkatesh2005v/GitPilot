package com.example.gitpilot.team.dto;

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
public class BusFactorReportDto {
    private Long repositoryId;
    private int totalContributors;
    private int busFactorScore; // e.g. 1 (High risk - 1 developer owns most code) to 5+ (Distributed)
    private String overallRiskLevel; // CRITICAL, HIGH, MEDIUM, LOW
    @Builder.Default
    private List<ContributorOwnershipDto> contributorOwnerships = new ArrayList<>();
    @Builder.Default
    private List<String> highRiskModules = new ArrayList<>();
    @Builder.Default
    private List<String> teamRecommendations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributorOwnershipDto {
        private String authorName;
        private String authorEmail;
        private int commitCount;
        private double ownershipPercentage;
        private List<String> primaryModules;
    }
}
