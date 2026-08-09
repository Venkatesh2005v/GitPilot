package com.example.gitpilot.memory.service;

import com.example.gitpilot.memory.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeSearchService {

    private final RepositoryJourneyService journeyService;
    private final TimelineService timelineService;
    private final OnboardingService onboardingService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final EngineeringDecisionService decisionService;

    public KnowledgeSearchService(RepositoryJourneyService journeyService,
                                  TimelineService timelineService,
                                  OnboardingService onboardingService,
                                  KnowledgeGraphService knowledgeGraphService,
                                  EngineeringDecisionService decisionService) {
        this.journeyService = journeyService;
        this.timelineService = timelineService;
        this.onboardingService = onboardingService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.decisionService = decisionService;
    }

    public KnowledgeSearchResultDto searchKnowledge(Long repositoryId, String query) {
        if (query == null || query.isBlank()) {
            return KnowledgeSearchResultDto.builder()
                    .query("")
                    .totalMatches(0)
                    .items(List.of())
                    .build();
        }

        String q = query.trim().toLowerCase();
        List<KnowledgeSearchResultDto.SearchResultItem> items = new ArrayList<>();

        // 1. Search Engineering Decisions
        List<EngineeringDecisionDto> decisions = decisionService.getDecisions(repositoryId);
        for (EngineeringDecisionDto d : decisions) {
            if (matches(q, d.getDecisionTitle(), d.getRationale(), d.getCategory(), d.getImpactSummary())) {
                items.add(KnowledgeSearchResultDto.SearchResultItem.builder()
                        .type("ENGINEERING_DECISION")
                        .title(d.getDecisionTitle())
                        .snippet(d.getRationale())
                        .category(d.getCategory())
                        .linkUrl("/journey")
                        .matchScore(0.95)
                        .build());
            }
        }

        // 2. Search Onboarding Steps
        List<OnboardingStepDto> steps = onboardingService.getOnboardingRoadmap(repositoryId);
        for (OnboardingStepDto s : steps) {
            if (matches(q, s.getModuleName(), s.getTitle(), s.getWhyItMatters())) {
                items.add(KnowledgeSearchResultDto.SearchResultItem.builder()
                        .type("ONBOARDING_MODULE")
                        .title("Step " + s.getStepOrder() + ": " + s.getTitle())
                        .snippet(s.getWhyItMatters())
                        .category(s.getDifficulty())
                        .linkUrl("/onboarding")
                        .matchScore(0.90)
                        .build());
            }
        }

        // 3. Search Knowledge Nodes
        KnowledgeMapDto map = knowledgeGraphService.getKnowledgeMap(repositoryId);
        for (KnowledgeMapDto.NodeDto n : map.getNodes()) {
            if (matches(q, n.getName(), n.getCategory(), n.getDescription(), n.getNodeKey())) {
                items.add(KnowledgeSearchResultDto.SearchResultItem.builder()
                        .type("KNOWLEDGE_NODE")
                        .title(n.getName())
                        .snippet(n.getDescription())
                        .category(n.getCategory())
                        .linkUrl("/knowledge")
                        .matchScore(0.85)
                        .build());
            }
        }

        // 4. Search Milestones / Journeys
        List<RepositoryJourneyDto> journeys = journeyService.getJourney(repositoryId, null, null, null, null);
        for (RepositoryJourneyDto j : journeys) {
            if (matches(q, j.getMilestoneName(), j.getDescription(), j.getMilestoneCategory())) {
                items.add(KnowledgeSearchResultDto.SearchResultItem.builder()
                        .type("MILESTONE")
                        .title(j.getMilestoneName())
                        .snippet(j.getDescription())
                        .category(j.getMilestoneCategory())
                        .linkUrl("/journey")
                        .matchScore(0.80)
                        .build());
            }
        }

        return KnowledgeSearchResultDto.builder()
                .query(query)
                .totalMatches(items.size())
                .items(items)
                .build();
    }

    private boolean matches(String q, String... fields) {
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(q)) {
                return true;
            }
        }
        return false;
    }
}
