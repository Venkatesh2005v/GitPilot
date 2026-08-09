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
public class RepositorySummaryDto {
    private String projectPurpose;
    private List<String> primaryTechnologies;
    private List<String> frameworksDetected;
    private String architectureStyle;
    private String mainFunctionality;
    private String complexityLevel; // Low, Medium, High, Enterprise
    private String lastUpdatedInfo;
}
