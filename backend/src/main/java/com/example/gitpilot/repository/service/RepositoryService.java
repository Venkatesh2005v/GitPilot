package com.example.gitpilot.repository.service;

import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubRepositoryResponse;
import com.example.gitpilot.repository.dto.RepositorySelectionRequest;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.example.gitpilot.user.entity.User;
import com.example.gitpilot.user.repository.UserRepository;
import com.example.gitpilot.webhook.service.WebhookRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RepositoryService {

    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubClient githubClient;
    private final WebhookRegistrationService webhookRegistrationService;

    public RepositoryService(UserRepository userRepository,
                             RepositoryRepository repositoryRepository,
                             GithubClient githubClient,
                             WebhookRegistrationService webhookRegistrationService) {
        this.userRepository = userRepository;
        this.repositoryRepository = repositoryRepository;
        this.githubClient = githubClient;
        this.webhookRegistrationService = webhookRegistrationService;
    }

    @Transactional
    public void saveSelectedRepositories(
            RepositorySelectionRequest request,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2User oauthUser
    ) {
        // 1. Find authenticated user
        Map<String, Object> attributes = oauthUser.getAttributes();
        Long githubId = ((Number) attributes.get("id")).longValue();
        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with githubId: " + githubId));

        Set<Long> selectedIds = new HashSet<>(request.getGithubRepositoryIds() != null ? request.getGithubRepositoryIds() : List.of());
        log.info("[RepositorySelection] User={} selecting {} repositories: {}", user.getUsername(), selectedIds.size(), selectedIds);

        if (authorizedClient == null) {
            List<Repository> existingRepos = repositoryRepository.findByUser(user);
            for (Repository repo : existingRepos) {
                repo.setSelected(selectedIds.contains(repo.getGithubRepoId()));
            }
            repositoryRepository.saveAll(existingRepos);
            log.warn("[RepositorySelection] No OAuth client available - saved selections without webhook registration");
            return;
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        // 2. Fetch existing repositories
        List<Repository> existingRepos = repositoryRepository.findByUser(user);
        Set<Long> previouslySelectedIds = existingRepos.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSelected()))
                .map(Repository::getGithubRepoId)
                .collect(Collectors.toSet());

        for (Repository repo : existingRepos) {
            repo.setSelected(false);
        }

        Map<Long, Repository> repoMap = existingRepos.stream()
                .collect(Collectors.toMap(Repository::getGithubRepoId, repo -> repo));

        // 3. Fetch from GitHub
        List<GithubRepositoryResponse> githubRepos = githubClient.getRepositories(accessToken);
        List<GithubRepositoryResponse> selectedRepos = githubRepos.stream()
                .filter(repo -> selectedIds.contains(repo.getId()))
                .toList();

        List<Repository> reposToSave = new ArrayList<>(existingRepos);
        List<Repository> newlySelectedRepos = new ArrayList<>();

        // 4. Upsert repositories
        for (GithubRepositoryResponse repoDto : selectedRepos) {
            Repository repository = repoMap.get(repoDto.getId());

            if (repository != null) {
                repository.setName(repoDto.getFullName() != null ? repoDto.getFullName() : repoDto.getName());
                repository.setDefaultBranch(repoDto.getDefaultBranch());
                repository.setHtmlUrl(repoDto.getHtmlUrl());
                repository.setPrivateRepo(repoDto.getIsPrivate());
                repository.setSelected(true);
                log.info("[RepositorySelection] Updated existing repo: repositoryId={} githubRepoId={} name={}",
                        repository.getId(), repository.getGithubRepoId(), repository.getName());
            } else {
                repository = new Repository();
                repository.setGithubRepoId(repoDto.getId());
                repository.setName(repoDto.getFullName() != null ? repoDto.getFullName() : repoDto.getName());
                repository.setDefaultBranch(repoDto.getDefaultBranch());
                repository.setHtmlUrl(repoDto.getHtmlUrl());
                repository.setPrivateRepo(repoDto.getIsPrivate());
                repository.setSelected(true);
                repository.setUser(user);
                reposToSave.add(repository);
                log.info("[RepositorySelection] Created new repo: githubRepoId={} name={}", repoDto.getId(), repository.getName());
            }

            // Track newly selected repos for webhook registration
            if (!previouslySelectedIds.contains(repoDto.getId())) {
                newlySelectedRepos.add(repository);
            }
        }

        // 5. Save all repositories
        List<Repository> saved = repositoryRepository.saveAll(reposToSave);
        log.info("[RepositorySelection] Saved {} repositories total", saved.size());

        // 6. Register webhooks for newly selected repositories
        for (Repository repo : newlySelectedRepos) {
            if (Boolean.TRUE.equals(repo.getSelected())) {
                webhookRegistrationService.ensureWebhookForRepository(repo, accessToken);
            }
        }
    }
}
