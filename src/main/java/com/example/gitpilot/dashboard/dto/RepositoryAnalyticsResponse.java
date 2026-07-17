package com.example.gitpilot.dashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RepositoryAnalyticsResponse {
    private Long id;
    private String repositoryName;
    private Long totalCommits;
    private LocalDateTime lastCommitDate;
    private Long uniqueContributorCount;

    // Polished fields populated afterward
    private LocalDateTime lastSyncedAt;
    private String lastSyncStatus;
    private Integer healthScore;
    private String aiProviderUsed;
    private LocalDateTime lastAIReportTime;

    public RepositoryAnalyticsResponse(Long id, String repositoryName, Long totalCommits, LocalDateTime lastCommitDate, Long uniqueContributorCount) {
        this.id = id;
        this.repositoryName = repositoryName;
        this.totalCommits = totalCommits;
        this.lastCommitDate = lastCommitDate;
        this.uniqueContributorCount = uniqueContributorCount;
    }
}
