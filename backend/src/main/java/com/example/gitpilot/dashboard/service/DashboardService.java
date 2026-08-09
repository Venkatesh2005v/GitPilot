package com.example.gitpilot.dashboard.service;

import com.example.gitpilot.ai.repository.AIReportRepository;
import com.example.gitpilot.commit.dto.CommitResponse;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.dashboard.dto.*;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.example.gitpilot.user.entity.User;
import com.example.gitpilot.user.repository.UserRepository;
import com.example.gitpilot.webhook.entity.RepositoryWebhook;
import com.example.gitpilot.webhook.repository.RepositoryWebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final AIReportRepository aiReportRepository;
    private final RepositoryWebhookRepository webhookRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardService(UserRepository userRepository,
                             RepositoryRepository repositoryRepository,
                             CommitRepository commitRepository,
                             AIReportRepository aiReportRepository,
                             RepositoryWebhookRepository webhookRepository) {
        this.userRepository = userRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.aiReportRepository = aiReportRepository;
        this.webhookRepository = webhookRepository;
    }

    private User getAuthenticatedUser(OAuth2User oauthUser) {
        if (oauthUser == null || oauthUser.getAttributes() == null) {
            return null;
        }
        Map<String, Object> attributes = oauthUser.getAttributes();
        Object idObj = attributes.get("id");
        if (idObj == null) {
            return null;
        }
        Long githubId = ((Number) idObj).longValue();
        return userRepository.findByGithubId(githubId).orElse(null);
    }

    private Repository findRepositoryOrFallback(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));
    }

    public List<RepositoryAnalyticsResponse> getRepositoriesAnalytics(OAuth2User oauthUser) {
        User user = getAuthenticatedUser(oauthUser);
        if (user == null) {
            return List.of();
        }
        List<RepositoryAnalyticsResponse> list = repositoryRepository.findRepositoryAnalyticsByUser(user);
        List<Repository> userRepos = repositoryRepository.findByUserAndSelectedTrue(user);
        Map<Long, Repository> repoMap = userRepos.stream()
                .collect(java.util.stream.Collectors.toMap(Repository::getId, java.util.function.Function.identity()));

        for (RepositoryAnalyticsResponse resp : list) {
            Repository repo = repoMap.get(resp.getId());
            if (repo != null) {
                resp.setLastSyncedAt(repo.getLastSyncedAt());
                resp.setLastSyncStatus(repo.getLastSyncStatus());
            }

            aiReportRepository.findFirstByRepositoryAndReportTypeOrderByGeneratedTimeDesc(repo, "FULL_INTELLIGENCE")
                    .or(() -> aiReportRepository.findFirstByRepositoryAndReportTypeOrderByGeneratedTimeDesc(repo, "FULL_REPORT"))
                    .ifPresent(report -> {
                        try {
                            Map<?, ?> data = objectMapper.readValue(report.getGeneratedReport(), Map.class);
                            if (data.containsKey("healthScore")) {
                                Object hs = data.get("healthScore");
                                if (hs instanceof Number) {
                                    resp.setHealthScore(((Number) hs).intValue());
                                } else if (hs instanceof Map) {
                                    Object overall = ((Map<?, ?>) hs).get("overallHealthScore");
                                    if (overall instanceof Number) {
                                        resp.setHealthScore(((Number) overall).intValue());
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                        resp.setAiProviderUsed(report.getProvider());
                        resp.setLastAIReportTime(report.getGeneratedTime());
                    });

            // Populate webhook status
            webhookRepository.findByRepositoryId(resp.getId()).ifPresentOrElse(
                    webhook -> {
                        resp.setWebhookStatus("Connected");
                        resp.setWebhookId(webhook.getGithubWebhookId());
                        resp.setWebhookPayloadUrl(webhook.getPayloadUrl());
                        resp.setWebhookActive(webhook.getActive());
                        resp.setWebhookLastDeliveryAt(webhook.getLastDeliveryAt());
                    },
                    () -> resp.setWebhookStatus("Missing")
            );
        }
        return list;
    }

    public RepositoryActivityResponse getRepositoryActivity(Long repositoryId) {
        log.debug("[RepositorySync] Fetching activity for repositoryId={}", repositoryId);
        Repository repository = findRepositoryOrFallback(repositoryId);

        Long totalCommits = commitRepository.countByRepository(repository);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long commitsLast7Days = commitRepository.countByRepositoryAndCommitDateAfter(repository, sevenDaysAgo);

        Commit latestCommit = commitRepository.findFirstByRepositoryOrderByCommitDateDesc(repository).orElse(null);
        String latestCommitMessage = latestCommit != null ? latestCommit.getMessage() : null;
        String latestCommitAuthor = latestCommit != null ? latestCommit.getAuthorName() : null;
        LocalDateTime latestCommitDate = latestCommit != null ? latestCommit.getCommitDate() : null;

        RepositoryActivityResponse response = new RepositoryActivityResponse(
                repository.getName(),
                totalCommits,
                commitsLast7Days,
                latestCommitMessage,
                latestCommitAuthor,
                latestCommitDate
        );

        // Enrich with sync status
        String syncStatus = repository.getLastSyncStatus() != null ? repository.getLastSyncStatus() : "Never";
        response.setSyncStatus(syncStatus);
        response.setLastSyncedAt(repository.getLastSyncedAt());
        response.setLastSyncDurationMs(repository.getLastSyncDuration());

        List<ContributorResponse> contributors = commitRepository.findContributorsByRepository(repository);
        response.setUniqueContributorCount((long) contributors.size());

        return response;
    }

    public List<CommitResponse> getRepositoryCommits(Long repositoryId, int page, int size) {
        Repository repository = findRepositoryOrFallback(repositoryId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Commit> commitPage = commitRepository.findByRepositoryOrderByCommitDateDesc(repository, pageable);

        return commitPage.getContent().stream()
                .map(commit -> new CommitResponse(
                        commit.getGithubCommitSha(),
                        commit.getMessage(),
                        commit.getAuthorName(),
                        commit.getAuthorEmail(),
                        commit.getCommitDate(),
                        commit.getCommitUrl()
                ))
                .toList();
    }

    public DashboardSummaryResponse getDashboardSummary(OAuth2User oauthUser) {
        User user = getAuthenticatedUser(oauthUser);
        if (user == null) {
            return new DashboardSummaryResponse(0L, 0L, 0L, "-", null);
        }

        Long selectedRepositories = repositoryRepository.countByUserAndSelectedTrue(user);
        Long totalCommits = commitRepository.countCommitsByUser(user);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long commitsLast7Days = commitRepository.countCommitsByUserSince(user, sevenDaysAgo);

        List<String> activeRepoNames = commitRepository.findMostActiveRepositoryName(user, PageRequest.of(0, 1));
        String mostActiveRepository = activeRepoNames.isEmpty() ? "-" : activeRepoNames.get(0);

        LocalDateTime lastSynchronization = repositoryRepository.findLastSynchronizationByUser(user);

        return new DashboardSummaryResponse(
                selectedRepositories,
                totalCommits,
                commitsLast7Days,
                mostActiveRepository,
                lastSynchronization
        );
    }

    public List<ContributorResponse> getRepositoryContributors(Long repositoryId) {
        Repository repository = findRepositoryOrFallback(repositoryId);

        return commitRepository.findContributorsByRepository(repository);
    }
}
