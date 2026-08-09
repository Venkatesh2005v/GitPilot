package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommitSummaryDto {
    private String highLevelSummary;
    private List<String> keyChanges; // e.g. "Authentication module was improved.", "Webhook processing was added."
    private Integer recentCommitCount;
    private String latestCommitAuthor;
    private String activityTrend; // High, Moderate, Low
}
