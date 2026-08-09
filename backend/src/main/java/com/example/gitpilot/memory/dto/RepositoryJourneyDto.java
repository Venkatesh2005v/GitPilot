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
public class RepositoryJourneyDto {
    private Long id;
    private Long repositoryId;
    private String milestoneName;
    private String milestoneCategory;
    private String iconName;
    private String description;
    private LocalDateTime eventDate;
    private String releaseVersion;
    private String contributor;
    private String monthVal;
    private Integer yearVal;
}
