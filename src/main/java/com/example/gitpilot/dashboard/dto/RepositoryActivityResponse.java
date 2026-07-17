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
}
