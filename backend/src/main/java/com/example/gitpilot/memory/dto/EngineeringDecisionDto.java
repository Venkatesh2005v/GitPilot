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
public class EngineeringDecisionDto {
    private Long id;
    private Long repositoryId;
    private String decisionTitle;
    private String rationale;
    private String status;
    private String category;
    private LocalDateTime dateInferred;
    private String commitSha;
    private String impactSummary;
}
