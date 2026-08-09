package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImprovementSuggestionDto {
    private String title;          // e.g. "Add Unit Testing"
    private String category;       // Testing, Security, DevOps, Documentation, Performance, Code Quality
    private String priority;       // HIGH, MEDIUM, LOW
    private String description;    // Actionable rationale
}
