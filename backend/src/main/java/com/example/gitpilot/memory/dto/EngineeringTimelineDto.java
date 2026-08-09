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
public class EngineeringTimelineDto {
    private Long id;
    private Long repositoryId;
    private String timePeriod;
    private String milestoneTitle;
    private String summary;
    private String impactLevel;
    private Integer commitCount;
    private LocalDateTime createdAt;
}
