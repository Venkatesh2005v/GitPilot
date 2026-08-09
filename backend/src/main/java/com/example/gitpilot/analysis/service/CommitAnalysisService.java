package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.CommitSummaryDto;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommitAnalysisService {

    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommitAnalysisService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public CommitSummaryDto analyzeCommits(Repository repository, List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return CommitSummaryDto.builder()
                    .highLevelSummary("No commit history recorded yet for this repository.")
                    .keyChanges(List.of("Awaiting initial commit synchronization"))
                    .recentCommitCount(0)
                    .latestCommitAuthor("N/A")
                    .activityTrend("Low")
                    .build();
        }

        List<String> commitMsgs = commits.stream()
                .map(Commit::getMessage)
                .filter(msg -> msg != null && !msg.trim().isEmpty())
                .limit(20)
                .toList();

        String latestAuthor = commits.get(0).getAuthorName() != null ? commits.get(0).getAuthorName() : "Developer";

        String prompt = String.format(
                "You are an AI Developer Workflow Analyst. Analyze the following list of recent commit messages for repository '%s':\n" +
                "Commits:\n%s\n\n" +
                "Instead of listing every single commit, group and synthesize them into high-level change narratives such as:\n" +
                "- \"Authentication module was improved.\"\n" +
                "- \"Webhook processing was added.\"\n" +
                "- \"Repository synchronization was optimized.\"\n\n" +
                "Return ONLY a valid raw JSON object matching this structure (no markdown fences, no backticks):\n" +
                "{\n" +
                "  \"highLevelSummary\": \"Recent engineering activity focused on refining API routes, stability, and intelligence features.\",\n" +
                "  \"keyChanges\": [\n" +
                "    \"Authentication and OAuth flow were enhanced.\",\n" +
                "    \"Webhook event routing with Strategy pattern was implemented.\",\n" +
                "    \"Repository synchronization performance was optimized.\"\n" +
                "  ],\n" +
                "  \"recentCommitCount\": %d,\n" +
                "  \"latestCommitAuthor\": \"%s\",\n" +
                "  \"activityTrend\": \"High\"\n" +
                "}",
                repository.getName(),
                String.join("\n", commitMsgs),
                commits.size(),
                latestAuthor
        );

        try {
            AIGatewayService.AIResult result = aiGatewayService.generateInsightWithFailover(prompt);
            String rawJson = cleanJsonResponse(result.text);
            CommitSummaryDto dto = objectMapper.readValue(rawJson, CommitSummaryDto.class);
            dto.setRecentCommitCount(commits.size());
            dto.setLatestCommitAuthor(latestAuthor);
            return dto;
        } catch (Exception e) {
            List<String> fallbackChanges = new ArrayList<>();
            for (String msg : commitMsgs) {
                if (msg.toLowerCase().contains("auth") || msg.toLowerCase().contains("security")) {
                    fallbackChanges.add("Authentication module was improved.");
                } else if (msg.toLowerCase().contains("webhook")) {
                    fallbackChanges.add("Webhook processing was added.");
                } else if (msg.toLowerCase().contains("sync") || msg.toLowerCase().contains("commit")) {
                    fallbackChanges.add("Repository synchronization was optimized.");
                } else if (msg.toLowerCase().contains("test")) {
                    fallbackChanges.add("Test suite coverage was expanded.");
                } else if (msg.toLowerCase().contains("doc") || msg.toLowerCase().contains("readme")) {
                    fallbackChanges.add("Project documentation was updated.");
                }
            }
            if (fallbackChanges.isEmpty()) {
                fallbackChanges.add("Core application features and maintenance updates were applied.");
                fallbackChanges.add("Repository configuration and dependency management were updated.");
            }

            return CommitSummaryDto.builder()
                    .highLevelSummary("Active development with regular commits improving platform infrastructure and features.")
                    .keyChanges(fallbackChanges.stream().distinct().toList())
                    .recentCommitCount(commits.size())
                    .latestCommitAuthor(latestAuthor)
                    .activityTrend(commits.size() > 10 ? "High" : commits.size() > 3 ? "Moderate" : "Low")
                    .build();
        }
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```")) trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        return trimmed.trim();
    }
}
