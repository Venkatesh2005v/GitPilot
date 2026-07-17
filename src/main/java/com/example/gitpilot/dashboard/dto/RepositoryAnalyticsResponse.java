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
public class RepositoryAnalyticsResponse {
    private Long id;
    private String repositoryName;
    private Long totalCommits;
    private LocalDateTime lastCommitDate;
    private Long uniqueContributorCount;
}
