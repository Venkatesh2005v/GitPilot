package com.example.gitpilot.commit.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubCommitResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommitService {

    private final CommitRepository commitRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubClient githubClient;
    
    // In-memory set to lock concurrent synchronizations per repository
    private final Set<Long> syncingRepositoryIds = ConcurrentHashMap.newKeySet();

    public CommitService(CommitRepository commitRepository,
                         RepositoryRepository repositoryRepository,
                         GithubClient githubClient) {
        this.commitRepository = commitRepository;
        this.repositoryRepository = repositoryRepository;
        this.githubClient = githubClient;
    }

    @Transactional
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
                throw new IllegalArgumentException("Invalid repository HTML URL");
            }
            String path = htmlUrl.substring(htmlUrl.indexOf("github.com/") + 11);
            String[] parts = path.split("/");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid repository path in HTML URL: " + htmlUrl);
            }
            String owner = parts[0];
            String repositoryName = parts[1];

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            List<GithubCommitResponse> githubCommits = githubClient.getCommits(owner, repositoryName, accessToken);

            int count = 0;
            for (GithubCommitResponse commitDto : githubCommits) {
                if (commitRepository.findByGithubCommitSha(commitDto.getSha()).isPresent()) {
                    continue;
                }

                Commit commit = new Commit();
                commit.setGithubCommitSha(commitDto.getSha());
                commit.setCommitUrl(commitDto.getHtmlUrl());

                if (commitDto.getCommit() != null) {
                    commit.setMessage(commitDto.getCommit().getMessage());
                    if (commitDto.getCommit().getAuthor() != null) {
                        commit.setAuthorName(commitDto.getCommit().getAuthor().getName());
                        commit.setAuthorEmail(commitDto.getCommit().getAuthor().getEmail());
                        commit.setCommitDate(commitDto.getCommit().getAuthor().getDate());
                    }
                }

                commit.setRepository(repository);
                commitRepository.save(commit);
                count++;
            }

            long duration = System.currentTimeMillis() - startTime;
            repository.setLastSyncedAt(LocalDateTime.now());
            repository.setLastSyncStatus("SUCCESS");
            repository.setLastSyncDuration(duration);
            repository.setLastSyncError(null);
            repositoryRepository.save(repository);

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
