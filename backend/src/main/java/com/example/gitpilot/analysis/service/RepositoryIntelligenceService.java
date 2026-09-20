package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.entity.AIReport;
import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.ai.repository.AIReportRepository;
import com.example.gitpilot.analysis.dto.*;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RepositoryIntelligenceService {

    public static final String REPORT_TYPE_INTELLIGENCE = "FULL_INTELLIGENCE";

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final AIReportRepository aiReportRepository;
    private final TechnologyDetectionService technologyDetectionService;
    private final RepositoryAnalysisService repositoryAnalysisService;
    private final ReadmeAnalysisService readmeAnalysisService;
    private final CommitAnalysisService commitAnalysisService;
    private final HealthScoreService healthScoreService;
    private final ImprovementSuggestionsService improvementSuggestionsService;
    private final AIGatewayService aiGatewayService;
    private final com.example.gitpilot.github.client.GithubClient githubClient;
    private final RepositoryEvidenceService repositoryEvidenceService;
    private final ObjectMapper objectMapper;

    public RepositoryIntelligenceService(RepositoryRepository repositoryRepository,
                                         CommitRepository commitRepository,
                                         AIReportRepository aiReportRepository,
                                         TechnologyDetectionService technologyDetectionService,
                                         RepositoryAnalysisService repositoryAnalysisService,
                                         ReadmeAnalysisService readmeAnalysisService,
                                         CommitAnalysisService commitAnalysisService,
                                         HealthScoreService healthScoreService,
                                         ImprovementSuggestionsService improvementSuggestionsService,
                                         AIGatewayService aiGatewayService,
                                         com.example.gitpilot.github.client.GithubClient githubClient,
                                         RepositoryEvidenceService repositoryEvidenceService) {
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.aiReportRepository = aiReportRepository;
        this.technologyDetectionService = technologyDetectionService;
        this.repositoryAnalysisService = repositoryAnalysisService;
        this.readmeAnalysisService = readmeAnalysisService;
        this.commitAnalysisService = commitAnalysisService;
        this.healthScoreService = healthScoreService;
        this.improvementSuggestionsService = improvementSuggestionsService;
        this.aiGatewayService = aiGatewayService;
        this.githubClient = githubClient;
        this.repositoryEvidenceService = repositoryEvidenceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional
    public RepositoryIntelligenceResponse getRepositoryIntelligence(Long repositoryId, OAuth2AuthorizedClient authorizedClient, boolean forceRefresh) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        // 1. Return cached AI response from PostgreSQL unless forceRefresh is explicitly requested
        if (!forceRefresh) {
            Optional<AIReport> cachedReportOpt = aiReportRepository.findFirstByRepositoryAndReportTypeOrderByGeneratedTimeDesc(repository, REPORT_TYPE_INTELLIGENCE);
            if (cachedReportOpt.isPresent()) {
                try {
                    AIReport dbReport = cachedReportOpt.get();
                    RepositoryIntelligenceResponse response = objectMapper.readValue(dbReport.getGeneratedReport(), RepositoryIntelligenceResponse.class);
                    response.setCached(true);
                    return response;
                } catch (Exception ignored) {}
            }
        }

        // 2. Generate Fresh AI Insights with fail-safe fallback
        List<Commit> commits = commitRepository.findByRepositoryOrderByCommitDateDesc(repository);
        List<String> commitMsgs = commits.stream().map(Commit::getMessage).toList();

        String accessToken = authorizedClient != null ? authorizedClient.getAccessToken().getTokenValue() : null;

        log.info("[Intelligence] repositoryId={} htmlUrl={} accessToken={}", 
                repository.getId(), repository.getHtmlUrl(), accessToken != null ? "present" : "NULL");

        // Fetch actual README from GitHub
        String readmeContent = "";
        String owner = "";
        String repoShort = "";
        if (repository.getHtmlUrl() != null && repository.getHtmlUrl().contains("github.com/")) {
            String urlPath = repository.getHtmlUrl().substring(repository.getHtmlUrl().indexOf("github.com/") + 11);
            // Trim trailing slashes, query params, and .git suffix
            if (urlPath.contains("?")) urlPath = urlPath.substring(0, urlPath.indexOf("?"));
            if (urlPath.endsWith("/")) urlPath = urlPath.substring(0, urlPath.length() - 1);
            if (urlPath.endsWith(".git")) urlPath = urlPath.substring(0, urlPath.length() - 4);
            String[] parts = urlPath.split("/");
            if (parts.length >= 2) {
                owner = parts[0];
                repoShort = parts[1];
            }
        }

        log.info("[Intelligence] Resolved owner='{}' repo='{}' from htmlUrl='{}'", owner, repoShort, repository.getHtmlUrl());

        if (!owner.isEmpty() && !repoShort.isEmpty() && accessToken != null) {
            try {
                readmeContent = githubClient.getReadme(owner, repoShort, accessToken);
                log.info("[README] Fetched for {}/{} length={}", owner, repoShort, readmeContent.length());
            } catch (Exception e) {
                log.warn("[README] Not found or error for {}/{}: {}", owner, repoShort, e.getMessage());
            }
        } else {
            log.warn("[README] Skipped fetch: owner='{}' repo='{}' token={}", owner, repoShort, accessToken != null);
        }

        TechStackDto techStack;
        RepositorySummaryDto summary;
        ReadmeSummaryDto readmeSummary;
        CommitSummaryDto commitSummary;
        HealthScoreDto healthScore;
        List<ImprovementSuggestionDto> suggestions;

        try {
            techStack = technologyDetectionService.detectTechStack(repository, accessToken, readmeContent, commitMsgs);
            // Detect concrete repository evidence (tests/CI/Docker) once, reused by health scoring
            // and the deterministic recommendation fallback. Fails safe (unresolved) if unavailable.
            RepositoryEvidenceService.RepositoryEvidence evidence =
                    repositoryEvidenceService.detect(owner, repoShort, accessToken, techStack.getDetectedManifestFiles());
            summary = repositoryAnalysisService.analyzeRepository(repository, techStack, commitMsgs, readmeContent);
            readmeSummary = readmeAnalysisService.analyzeReadme(repository, readmeContent);
            commitSummary = commitAnalysisService.analyzeCommits(repository, commits);
            healthScore = healthScoreService.calculateHealthScore(repository, techStack, readmeContent, commits, evidence);
            suggestions = improvementSuggestionsService.generateSuggestions(repository, techStack, healthScore, readmeContent, evidence);
        } catch (Exception e) {
            // Graceful fallback: return minimal data indicating AI analysis is unavailable
            techStack = TechStackDto.builder()
                    .detectedTechnologies(List.of())
                    .detectedManifestFiles(List.of())
                    .primaryLanguage("Unknown")
                    .category("Unknown")
                    .build();

            summary = RepositorySummaryDto.builder()
                    .projectPurpose("AI analysis temporarily unavailable. Please retry or use the refresh button.")
                    .primaryTechnologies(List.of())
                    .frameworksDetected(List.of())
                    .architectureStyle("Unknown")
                    .mainFunctionality("Unable to generate summary at this time.")
                    .complexityLevel("Unknown")
                    .lastUpdatedInfo("Analysis failed")
                    .build();

            readmeSummary = ReadmeSummaryDto.builder()
                    .readmePresent(false)
                    .projectOverview("Unable to analyze README at this time.")
                    .features(List.of())
                    .installationSummary("")
                    .architectureSummary("")
                    .usageSummary("")
                    .build();

            commitSummary = CommitSummaryDto.builder()
                    .highLevelSummary("AI analysis unavailable.")
                    .keyChanges(List.of())
                    .recentCommitCount(commits.size())
                    .latestCommitAuthor(commits.isEmpty() ? "N/A" : commits.get(0).getAuthorName())
                    .activityTrend("Unknown")
                    .build();

            healthScore = HealthScoreDto.builder()
                    .overallHealthScore(0)
                    .documentationRating("N/A")
                    .testingRating("N/A")
                    .architectureRating("N/A")
                    .maintainabilityRating("N/A")
                    .keyStrengths(List.of())
                    .keyWeaknesses(List.of("AI analysis failed - retry to generate insights"))
                    .build();

            suggestions = List.of(
                    ImprovementSuggestionDto.builder()
                            .title("Retry AI Analysis")
                            .category("System")
                            .priority("HIGH")
                            .description("AI analysis failed. Click refresh to retry generating insights for this repository.")
                            .build()
            );
        }

        String providerName = "Google Gemini";
        String modelName = "gemini-2.5-flash";
        String fallbackUsed = "None";
        LocalDateTime now = LocalDateTime.now();

        RepositoryIntelligenceResponse response = RepositoryIntelligenceResponse.builder()
                .repositoryId(repository.getId())
                .repositoryName(repository.getName())
                .summary(summary)
                .techStack(techStack)
                .readmeSummary(readmeSummary)
                .commitSummary(commitSummary)
                .suggestions(suggestions)
                .healthScore(healthScore)
                .providerUsed(providerName)
                .modelUsed(modelName)
                .fallbackUsed(fallbackUsed)
                .generatedTime(now)
                .cached(false)
                .analysisConfidence(computeConfidence(readmeSummary, techStack, commitSummary, healthScore, summary))
                .grade(healthScore.getGrade())
                .aiExplanation(healthScore.getAiExplanation())
                .build();

        // 3. Store generated insight in PostgreSQL DB cache
        try {
            String jsonContent = objectMapper.writeValueAsString(response);
            AIReport report = new AIReport();
            report.setRepository(repository);
            report.setReportType(REPORT_TYPE_INTELLIGENCE);
            report.setProvider(providerName);
            report.setModel(modelName);
            report.setFallbackUsed(fallbackUsed);
            report.setGeneratedReport(jsonContent);
            report.setGeneratedTime(now);

            log.info("[AI] Persisting AI report: repositoryId={} reportType={} generatedTime={}", repository.getId(), REPORT_TYPE_INTELLIGENCE, now);
            AIReport saved = aiReportRepository.save(report);
            aiReportRepository.flush();
            log.info("[AI] AI report persisted successfully: id={} repositoryId={}", saved.getId(), repository.getId());
        } catch (Exception e) {
            log.error("[AI] Failed to persist AI report for repositoryId={}: {}", repository.getId(), e.getMessage(), e);
        }

        return response;
    }

    private int computeConfidence(com.example.gitpilot.analysis.dto.ReadmeSummaryDto readme,
                                   com.example.gitpilot.analysis.dto.TechStackDto tech,
                                   com.example.gitpilot.analysis.dto.CommitSummaryDto commits,
                                   com.example.gitpilot.analysis.dto.HealthScoreDto health,
                                   com.example.gitpilot.analysis.dto.RepositorySummaryDto summary) {
        int completed = 0;
        int total = 7;
        if (readme != null && Boolean.TRUE.equals(readme.getReadmePresent())) completed++;
        if (tech != null && tech.getDetectedTechnologies() != null && !tech.getDetectedTechnologies().isEmpty()) completed++;
        if (tech != null && tech.getPrimaryLanguage() != null && !tech.getPrimaryLanguage().equals("Unknown")) completed++;
        if (commits != null && commits.getRecentCommitCount() != null && commits.getRecentCommitCount() > 0) completed++;
        if (health != null && health.getOverallHealthScore() != null && health.getOverallHealthScore() > 0) completed++;
        if (summary != null && summary.getProjectPurpose() != null && !summary.getProjectPurpose().contains("unavailable")) completed++;
        if (commits != null && commits.getLatestCommitAuthor() != null && !commits.getLatestCommitAuthor().equals("N/A")) completed++;
        return (int) Math.round(((double) completed / total) * 100);
    }

    @Transactional
    public void invalidateCache(Long repositoryId) {
        repositoryRepository.findById(repositoryId).ifPresent(repo -> {
            try {
                aiReportRepository.deleteByRepositoryAndReportType(repo, REPORT_TYPE_INTELLIGENCE);
            } catch (Exception ignored) {}
        });
    }
}
