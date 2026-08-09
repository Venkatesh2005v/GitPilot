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
public class EngineeringMemoryOverviewDto {
    private Long repositoryId;
    private String repositoryName;
    private RepositoryDNADto dna;
    private List<RepositoryJourneyDto> recentJourneyMilestones;
    private List<EngineeringTimelineDto> monthlyTimelines;
    private List<OnboardingStepDto> onboardingRoadmap;
    private KnowledgeMapDto knowledgeMap;
    private List<EngineeringDecisionDto> keyDecisions;
    private List<RecommendationDto> recommendations;
}
