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
public class ReadmeSummaryDto {
    private Boolean readmePresent;
    private String projectOverview;
    private List<String> features;
    private String installationSummary;
    private String architectureSummary;
    private String usageSummary;
}
