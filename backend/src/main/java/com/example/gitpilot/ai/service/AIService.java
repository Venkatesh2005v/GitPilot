package com.example.gitpilot.ai.service;

import com.example.gitpilot.ai.cache.AICacheService;
import com.example.gitpilot.ai.cache.CacheEntry;
import com.example.gitpilot.ai.entity.AIReport;
import com.example.gitpilot.ai.repository.AIReportRepository;
import com.example.gitpilot.ai.dto.*;
import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.dashboard.dto.ContributorResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AIService {

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final AIGatewayService aiGatewayService;
    private final AICacheService aiCacheService;
    private final AIReportRepository aiReportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIService(RepositoryRepository repositoryRepository,
                     CommitRepository commitRepository,
                     AIGatewayService aiGatewayService,
                     AICacheService aiCacheService,
                     AIReportRepository aiReportRepository) {
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.aiGatewayService = aiGatewayService;
        this.aiCacheService = aiCacheService;
        this.aiReportRepository = aiReportRepository;
    }

    public static class ReportResult {
        public final Map<?, ?> data;
        public final String providerUsed;
        public final String model;
        public final String fallbackUsed;
        public final LocalDateTime generatedTime;

        public ReportResult(Map<?, ?> data, String providerUsed, String model, String fallbackUsed, LocalDateTime generatedTime) {
            this.data = data;
            this.providerUsed = providerUsed;
            this.model = model;
            this.fallbackUsed = fallbackUsed;
            this.generatedTime = generatedTime;
        }
    }

    private ReportResult generateOrFetchReport(Long repositoryId) {
        // 1. Try In-Memory Cache first
        CacheEntry entry = aiCacheService.get(repositoryId, "FULL_REPORT");
        if (entry != null) {
            log.info("[AI] Cache hit for repositoryId={} reportType=FULL_REPORT", repositoryId);
            try {
                Map<?, ?> data = objectMapper.readValue(entry.getContent(), Map.class);
                return new ReportResult(data, entry.getProviderUsed(), entry.getModel(), entry.getFallbackUsed(), entry.getGeneratedTime());
            } catch (Exception ignored) {}
        }

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        // 2. Try Database Cache (reuse within 1 hour TTL)
        Optional<AIReport> reportOpt = aiReportRepository.findFirstByRepositoryAndReportTypeOrderByGeneratedTimeDesc(repository, "FULL_REPORT");
        if (reportOpt.isPresent()) {
            AIReport dbReport = reportOpt.get();
            if (dbReport.getGeneratedTime().plusHours(1).isAfter(LocalDateTime.now())) {
                try {
                    Map<?, ?> data = objectMapper.readValue(dbReport.getGeneratedReport(), Map.class);
                    aiCacheService.put(repositoryId, "FULL_REPORT", dbReport.getGeneratedReport(), dbReport.getProvider(), dbReport.getModel(), dbReport.getFallbackUsed());
                    return new ReportResult(data, dbReport.getProvider(), dbReport.getModel(), dbReport.getFallbackUsed(), dbReport.getGeneratedTime());
                } catch (Exception ignored) {}
            }
        }

        // 3. Fallback: Call AI Gateway
        Long totalCommits = commitRepository.countByRepository(repository);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long commitsLast7Days = commitRepository.countByRepositoryAndCommitDateAfter(repository, sevenDaysAgo);

        List<ContributorResponse> contributors = commitRepository.findContributorsByRepository(repository);
        int contributorCount = contributors.size();

        Commit latestCommit = commitRepository.findFirstByRepositoryOrderByCommitDateDesc(repository).orElse(null);
        String latestCommitMsg = latestCommit != null ? latestCommit.getMessage() : "N/A";
        LocalDateTime latestCommitDate = latestCommit != null ? latestCommit.getCommitDate() : null;

        double commitFrequencyPerDay = totalCommits > 0 && repository.getCreatedAt() != null
                ? (double) totalCommits / Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(repository.getCreatedAt(), LocalDateTime.now()))
                : 0.0;

        String prompt = String.format(
                "You are an engineering analytics assistant. Analyze the following repository metrics:\n" +
                "Repository Name: %s\n" +
                "Total Commits: %d\n" +
                "Commits in Last 7 Days: %d\n" +
                "Contributor Count: %d\n" +
                "Latest Commit: %s\n" +
                "Latest Commit Date: %s\n" +
                "Commit Frequency (commits/day): %.2f\n" +
                "Last Synchronization Time: %s\n\n" +
                "Based on these metrics, generate:\n" +
                "1. A health score (0-100)\n" +
                "2. 2-3 major strengths\n" +
                "3. 2-3 weaknesses\n" +
                "4. 2-3 actionable engineering recommendations\n" +
                "5. A concise weekly engineering summary\n\n" +
                "Return ONLY a valid JSON object matching this structure (no markdown code blocks, no backticks):\n" +
                "{\n" +
                "  \"healthScore\": 85,\n" +
                "  \"strengths\": [\"Strength 1\", \"Strength 2\"],\n" +
                "  \"weaknesses\": [\"Weakness 1\", \"Weakness 2\"],\n" +
                "  \"recommendations\": [\"Rec 1\", \"Rec 2\"],\n" +
                "  \"summary\": \"The weekly engineering summary goes here...\"\n" +
                "}",
                repository.getName(), totalCommits, commitsLast7Days, contributorCount,
                latestCommitMsg, latestCommitDate != null ? latestCommitDate.toString() : "N/A",
                commitFrequencyPerDay, repository.getUpdatedAt() != null ? repository.getUpdatedAt().toString() : "N/A"
        );

        String providerUsed = "Google Gemini";
        String modelUsed = "gemini-2.5-flash";
        String fallbackUsed = "None";
        String rawResult;

        log.info("[AI] Cache miss for repositoryId={}. Generating fresh report via AI gateway.", repositoryId);

        try {
            AIGatewayService.AIResult aiResult = aiGatewayService.generateInsightWithFailover(prompt);
            rawResult = aiResult.text != null ? aiResult.text.trim() : "";
            providerUsed = aiResult.providerUsed;
            modelUsed = aiResult.modelUsed;
            fallbackUsed = aiResult.fallbackUsed;
            log.info("[AI] Generation complete for repositoryId={} provider={} model={}", repositoryId, providerUsed, modelUsed);
        } catch (Exception e) {
            log.error("[AI] All providers failed for repositoryId={}: {}", repositoryId, e.getMessage());
            rawResult = String.format("{\"healthScore\":0,\"strengths\":[],\"weaknesses\":[\"AI analysis unavailable\"],\"recommendations\":[\"Retry AI analysis when provider is available\"],\"summary\":\"AI analysis for %s is temporarily unavailable.\"}", repository.getName());
        }

        if (rawResult.startsWith("```json")) {
            rawResult = rawResult.substring(7);
        } else if (rawResult.startsWith("```")) {
            rawResult = rawResult.substring(3);
        }
        if (rawResult.endsWith("```")) {
            rawResult = rawResult.substring(0, rawResult.length() - 3);
        }
        rawResult = rawResult.trim();

        Map<?, ?> parsedData;
        try {
            parsedData = objectMapper.readValue(rawResult, Map.class);
        } catch (Exception e) {
            parsedData = Map.of(
                    "healthScore", 0,
                    "strengths", List.of(),
                    "weaknesses", List.of("AI response could not be parsed"),
                    "recommendations", List.of("Retry AI analysis"),
                    "summary", "AI analysis could not be completed for this repository."
            );
            try {
                rawResult = objectMapper.writeValueAsString(parsedData);
            } catch (Exception ignored) {}
        }

        LocalDateTime now = LocalDateTime.now();

        try {
            AIReport report = new AIReport();
            report.setRepository(repository);
            report.setReportType("FULL_REPORT");
            report.setProvider(providerUsed);
            report.setModel(modelUsed);
            report.setFallbackUsed(fallbackUsed);
            report.setGeneratedReport(rawResult);
            report.setGeneratedTime(now);
            log.info("[AI] Saving AI report for repositoryId={} reportType=FULL_REPORT provider={}", repositoryId, providerUsed);
            aiReportRepository.save(report);
            log.info("[AI] Saved AI report for repository {}", repository.getName());
        } catch (Exception e) {
            log.error("[AI] Failed saving AI report for repositoryId={}: {}", repositoryId, e.getMessage(), e);
        }

        aiCacheService.put(repositoryId, "FULL_REPORT", rawResult, providerUsed, modelUsed, fallbackUsed);

        return new ReportResult(parsedData, providerUsed, modelUsed, fallbackUsed, now);
    }

    @SuppressWarnings("unchecked")
    public AIHealthResponse getHealth(Long repositoryId) {
        ReportResult report = generateOrFetchReport(repositoryId);
        Object hs = report.data != null ? report.data.get("healthScore") : null;
        Integer healthScore = hs instanceof Number ? ((Number) hs).intValue() : 0;

        List<String> strengths = report.data != null && report.data.get("strengths") instanceof List
                ? (List<String>) report.data.get("strengths")
                : List.of();

        List<String> weaknesses = report.data != null && report.data.get("weaknesses") instanceof List
                ? (List<String>) report.data.get("weaknesses")
                : List.of();

        return new AIHealthResponse(
                healthScore,
                strengths,
                weaknesses,
                report.providerUsed,
                report.model,
                report.fallbackUsed,
                report.generatedTime
        );
    }

    public AISummaryResponse getSummary(Long repositoryId) {
        ReportResult report = generateOrFetchReport(repositoryId);
        String summary = report.data != null && report.data.get("summary") != null
                ? (String) report.data.get("summary")
                : "No AI summary available. Please retry analysis.";

        return new AISummaryResponse(
                summary,
                report.providerUsed,
                report.model,
                report.fallbackUsed,
                report.generatedTime
        );
    }

    @SuppressWarnings("unchecked")
    public AIRecommendationsResponse getRecommendations(Long repositoryId) {
        ReportResult report = generateOrFetchReport(repositoryId);
        List<String> recommendations = report.data != null && report.data.get("recommendations") instanceof List
                ? (List<String>) report.data.get("recommendations")
                : List.of();

        return new AIRecommendationsResponse(
                recommendations,
                report.providerUsed,
                report.model,
                report.fallbackUsed,
                report.generatedTime
        );
    }
}
