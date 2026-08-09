package com.example.gitpilot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryActivityResponse {
    private String repositoryName;
    private Long totalCommits;
    private Long commitsLast7Days;
    private String latestCommitMessage;
    private String latestCommitAuthor;
    private LocalDateTime latestCommitDate;

    // Sync status
    private String syncStatus;       // Completed, Running, Failed, Never
    private LocalDateTime lastSyncedAt;
    private Long lastSyncDurationMs;
    private Long uniqueContributorCount;
    private Integer healthScore;

    public RepositoryActivityResponse(String repositoryName, Long totalCommits, Long commitsLast7Days,
                                       String latestCommitMessage, String latestCommitAuthor, LocalDateTime latestCommitDate) {
        this.repositoryName = repositoryName;
        this.totalCommits = totalCommits;
        this.commitsLast7Days = commitsLast7Days;
        this.latestCommitMessage = latestCommitMessage;
        this.latestCommitAuthor = latestCommitAuthor;
        this.latestCommitDate = latestCommitDate;
    }
}
