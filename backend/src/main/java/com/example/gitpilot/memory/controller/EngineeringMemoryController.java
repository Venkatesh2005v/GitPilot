package com.example.gitpilot.memory.controller;

import com.example.gitpilot.memory.dto.*;
import com.example.gitpilot.memory.service.*;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Engineering Memory Platform", description = "Endpoints for Phase 4 Repository Journey, DNA, Knowledge Maps, Decisions, Onboarding, and Search")
@RestController
@RequestMapping("/api/memory")
public class EngineeringMemoryController {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryJourneyService journeyService;
    private final TimelineService timelineService;
    private final RepositoryDNAService dnaService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final OnboardingService onboardingService;
    private final EngineeringDecisionService decisionService;
    private final RecommendationService recommendationService;
    private final KnowledgeSearchService searchService;

    public EngineeringMemoryController(RepositoryRepository repositoryRepository,
                                       RepositoryJourneyService journeyService,
                                       TimelineService timelineService,
                                       RepositoryDNAService dnaService,
                                       KnowledgeGraphService knowledgeGraphService,
                                       OnboardingService onboardingService,
                                       EngineeringDecisionService decisionService,
                                       RecommendationService recommendationService,
                                       KnowledgeSearchService searchService) {
        this.repositoryRepository = repositoryRepository;
        this.journeyService = journeyService;
        this.timelineService = timelineService;
        this.dnaService = dnaService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.onboardingService = onboardingService;
        this.decisionService = decisionService;
        this.recommendationService = recommendationService;
        this.searchService = searchService;
    }

    @Operation(summary = "Get Repository Journey Milestones", description = "Returns filtered visual engineering timeline milestones")
    @GetMapping("/{repositoryId}/journey")
    public ResponseEntity<List<RepositoryJourneyDto>> getJourney(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String release,
            @RequestParam(required = false) String contributor) {
        return ResponseEntity.ok(journeyService.getJourney(repositoryId, month, year, release, contributor));
    }

    @Operation(summary = "Get Engineering Timeline", description = "Returns commits grouped into monthly engineering milestones")
    @GetMapping("/{repositoryId}/timeline")
    public ResponseEntity<List<EngineeringTimelineDto>> getTimeline(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(timelineService.getTimeline(repositoryId));
    }

    @Operation(summary = "Get Repository DNA Personality Profile", description = "Returns 9-dimensional metrics for repository personality")
    @GetMapping("/{repositoryId}/dna")
    public ResponseEntity<RepositoryDNADto> getDNA(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(dnaService.getDNA(repositoryId));
    }

    @Operation(summary = "Get Developer Onboarding Roadmap", description = "Returns step-by-step onboarding roadmap with reading times and difficulty")
    @GetMapping("/{repositoryId}/onboarding")
    public ResponseEntity<List<OnboardingStepDto>> getOnboarding(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(onboardingService.getOnboardingRoadmap(repositoryId));
    }

    @Operation(summary = "Get Knowledge Map Graph", description = "Returns module nodes and relationship edges for visualization")
    @GetMapping("/{repositoryId}/knowledge-map")
    public ResponseEntity<KnowledgeMapDto> getKnowledgeMap(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(knowledgeGraphService.getKnowledgeMap(repositoryId));
    }

    @Operation(summary = "Get Inferred Engineering Decisions", description = "Returns inferred Architectural Decision Records (ADRs)")
    @GetMapping("/{repositoryId}/decisions")
    public ResponseEntity<List<EngineeringDecisionDto>> getDecisions(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(decisionService.getDecisions(repositoryId));
    }

    @Operation(summary = "Get Contextual Recommendations", description = "Returns prioritized repository-specific recommendations")
    @GetMapping("/{repositoryId}/recommendations")
    public ResponseEntity<List<RecommendationDto>> getRecommendations(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(repositoryId));
    }

    @Operation(summary = "Search Repository Knowledge", description = "Performs semantic search across modules, decisions, and onboarding steps")
    @GetMapping("/{repositoryId}/search")
    public ResponseEntity<KnowledgeSearchResultDto> searchKnowledge(
            @PathVariable Long repositoryId,
            @RequestParam String query) {
        return ResponseEntity.ok(searchService.searchKnowledge(repositoryId, query));
    }

    @Operation(summary = "Get Complete Engineering Memory Overview", description = "Returns aggregated payload for fast dashboard loading")
    @GetMapping("/{repositoryId}/overview")
    public ResponseEntity<EngineeringMemoryOverviewDto> getMemoryOverview(@PathVariable Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        EngineeringMemoryOverviewDto overview = EngineeringMemoryOverviewDto.builder()
                .repositoryId(repository.getId())
                .repositoryName(repository.getName())
                .dna(dnaService.getDNA(repositoryId))
                .recentJourneyMilestones(journeyService.getJourney(repositoryId, null, null, null, null))
                .monthlyTimelines(timelineService.getTimeline(repositoryId))
                .onboardingRoadmap(onboardingService.getOnboardingRoadmap(repositoryId))
                .knowledgeMap(knowledgeGraphService.getKnowledgeMap(repositoryId))
                .keyDecisions(decisionService.getDecisions(repositoryId))
                .recommendations(recommendationService.getRecommendations(repositoryId))
                .build();

        return ResponseEntity.ok(overview);
    }
}
