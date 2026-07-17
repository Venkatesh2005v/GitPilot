package com.example.gitpilot.commit.service;

import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncScheduler {

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

    // Every six hours (6 * 60 * 60 * 1000 ms)
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 10000)
    public void syncAllSelectedRepositories() {
        List<Repository> selectedRepos = repositoryRepository.findBySelectedTrue();
        for (Repository repo : selectedRepos) {
            if (repo.getUser() == null) {
                continue;
            }
            
            // Try to load authorized client by githubId first
            String principalName = repo.getUser().getGithubId().toString();
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principalName);
            if (authorizedClient == null) {
                // Fallback to username
                authorizedClient = authorizedClientService.loadAuthorizedClient("github", repo.getUser().getUsername());
            }

            if (authorizedClient != null) {
                try {
                    commitService.syncCommits(repo.getId(), authorizedClient);
                } catch (Exception ignored) {
                    // Failures are tracked and stored per repository inside CommitService
                }
            }
        }
    }
}
