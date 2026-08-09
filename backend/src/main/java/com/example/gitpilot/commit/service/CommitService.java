package com.example.gitpilot.commit.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubCommitResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommitService {

    private static final Logger log = LoggerFactory.getLogger(CommitService.class);

    private final CommitRepository commitRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubClient githubClient;
    private final com.example.gitpilot.ai.repository.AIReportRepository aiReportRepository;
    
    // In-memory set to lock concurrent synchronizations per repository
    private final Set<Long> syncingRepositoryIds = ConcurrentHashMap.newKeySet();

    public CommitService(CommitRepository commitRepository,
                         RepositoryRepository repositoryRepository,
                         GithubClient githubClient,
                         com.example.gitpilot.ai.repository.AIReportRepository aiReportRepository) {
        this.commitRepository = commitRepository;
        this.repositoryRepository = repositoryRepository;
        this.githubClient = githubClient;
        this.aiReportRepository = aiReportRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int syncCommits(Long repositoryId, OAuth2AuthorizedClient authorizedClient) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        if (!syncingRepositoryIds.add(repositoryId)) {
            throw new IllegalStateException("Synchronization already in progress for repository: " + repositoryId);
        }

        long startTime = System.currentTimeMillis();
        repository.setLastSyncStatus("RUNNING");
        repositoryRepository.saveAndFlush(repository);

        try {
            String htmlUrl = repository.getHtmlUrl();
            if (htmlUrl == null || !htmlUrl.contains("github.com/")) {
                throw new IllegalArgumentException("Invalid repository HTML URL: " + htmlUrl);
            }
            String path = htmlUrl.substring(htmlUrl.indexOf("github.com/") + 11);
            String[] parts = path.split("/");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid repository path in HTML URL: " + htmlUrl);
            }
            String owner = parts[0];
            String repositoryName = parts[1];

            List<GithubCommitResponse> githubCommits = null;
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                try {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    githubCommits = githubClient.getCommits(owner, repositoryName, accessToken);
                } catch (Exception e) {
                    log.warn("OAuth commit sync failed for {}/{}: {}", owner, repositoryName, e.getMessage());
                }
            }

            // Fallback to public GitHub API fetch if authorizedClient was absent or failed
            if (githubCommits == null || githubCommits.isEmpty()) {
                try {
                    githubCommits = githubClient.getPublicCommits(owner, repositoryName);
                } catch (Exception e) {
                    log.warn("Public commit fetch failed for {}/{}: {}", owner, repositoryName, e.getMessage());
                }
            }

            int count = 0;
            if (githubCommits != null && !githubCommits.isEmpty()) {
                for (GithubCommitResponse commitDto : githubCommits) {
                    if (commitRepository.findByGithubCommitSha(commitDto.getSha()).isPresent()) {
                        continue;
                    }

                    Commit commit = new Commit();
                    commit.setGithubCommitSha(commitDto.getSha());
                    commit.setCommitUrl(commitDto.getHtmlUrl() != null ? commitDto.getHtmlUrl() : "https://github.com/" + owner + "/" + repositoryName + "/commit/" + commitDto.getSha());

                    if (commitDto.getCommit() != null) {
                        commit.setMessage(commitDto.getCommit().getMessage());
                        if (commitDto.getCommit().getAuthor() != null) {
                            commit.setAuthorName(commitDto.getCommit().getAuthor().getName());
                            commit.setAuthorEmail(commitDto.getCommit().getAuthor().getEmail());
                            commit.setCommitDate(commitDto.getCommit().getAuthor().getDate());
                        }
                    }

                    if (commit.getAuthorName() == null || commit.getAuthorName().isBlank()) {
                        commit.setAuthorName(owner);
                    }
                    if (commit.getCommitDate() == null) {
                        commit.setCommitDate(LocalDateTime.now());
                    }

                    commit.setRepository(repository);
                    commitRepository.save(commit);
                    count++;
                }
            } else {
                log.info("[RepositorySync] No commits returned from GitHub for repository {}. Skipping.", repositoryId);
            }

            long duration = System.currentTimeMillis() - startTime;
            repository.setLastSyncedAt(LocalDateTime.now());
            repository.setLastSyncStatus("SUCCESS");
            repository.setLastSyncDuration(duration);
            repository.setLastSyncError(null);
            repositoryRepository.save(repository);

            try {
                aiReportRepository.deleteByRepositoryAndReportType(repository, "FULL_REPORT");
                aiReportRepository.deleteByRepositoryAndReportType(repository, "FULL_INTELLIGENCE");
            } catch (Exception ignored) {}

            return count;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            repository.setLastSyncedAt(LocalDateTime.now());
            repository.setLastSyncStatus("FAILED");
            repository.setLastSyncDuration(duration);
            repository.setLastSyncError(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            repositoryRepository.save(repository);
            throw e;
        } finally {
            syncingRepositoryIds.remove(repositoryId);
        }
    }
}
