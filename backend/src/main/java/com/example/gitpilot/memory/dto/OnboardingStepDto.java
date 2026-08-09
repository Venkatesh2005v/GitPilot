package com.example.gitpilot.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStepDto {
    private Long id;
    private Long repositoryId;
    private Integer stepOrder;
    private String moduleName;
    private String title;
    private String whyItMatters;
    private Integer readingTimeMinutes;
    private String difficulty;
    private List<String> dependencies;
    private List<String> keyFiles;
}
