package com.example.gitpilot.commit.service;

import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final RepositoryRepository repositoryRepository;
    private final CommitService commitService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public SyncScheduler(RepositoryRepository repositoryRepository,
                         CommitService commitService,
                         OAuth2AuthorizedClientService authorizedClientService) {
        this.repositoryRepository = repositoryRepository;
        this.commitService = commitService;
        this.authorizedClientService = authorizedClientService;
    }

    // Scheduled synchronization every six hours
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 10000)
    public void syncAllSelectedRepositories() {
        List<Repository> selectedRepos;
        try {
            // Eagerly fetch User association via JOIN FETCH to avoid LazyInitializationException outside transactional boundaries
            selectedRepos = repositoryRepository.findBySelectedTrueWithUser();
        } catch (Exception e) {
            log.error("Failed to fetch selected repositories for scheduled sync: {}", e.getMessage());
            return;
        }

        for (Repository repo : selectedRepos) {
            if (repo.getUser() == null) {
                continue;
            }

            // Skip sync if recently synchronized (within last 15 minutes)
            if (repo.getLastSyncedAt() != null && repo.getLastSyncedAt().plusMinutes(15).isAfter(java.time.LocalDateTime.now())) {
                continue;
            }
            
            OAuth2AuthorizedClient authorizedClient = null;
            if (repo.getUser().getGithubId() != null) {
                String principalName = repo.getUser().getGithubId().toString();
                authorizedClient = authorizedClientService.loadAuthorizedClient("github", principalName);
            }
            if (authorizedClient == null && repo.getUser().getUsername() != null) {
                authorizedClient = authorizedClientService.loadAuthorizedClient("github", repo.getUser().getUsername());
            }

            // Execute each repository synchronization in its own isolated transaction
            try {
                commitService.syncCommits(repo.getId(), authorizedClient);
            } catch (Exception e) {
                log.error("Error during scheduled synchronization for repository id {}: {}", repo.getId(), e.getMessage());
                // Single repository failure is isolated and does not abort remaining repositories
            }
        }
    }
}
