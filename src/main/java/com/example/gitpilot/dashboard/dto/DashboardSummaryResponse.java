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
public class DashboardSummaryResponse {
    private Long selectedRepositories;
    private Long totalCommits;
    private Long commitsLast7Days;
    private String mostActiveRepository;
    private LocalDateTime lastSynchronization;
}
