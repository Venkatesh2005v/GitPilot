package com.example.gitpilot.repository.service;

import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubRepositoryResponse;
import com.example.gitpilot.repository.dto.RepositorySelectionRequest;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.example.gitpilot.user.entity.User;
import com.example.gitpilot.user.repository.UserRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RepositoryService {

    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubClient githubClient;

    public RepositoryService(UserRepository userRepository,
                             RepositoryRepository repositoryRepository,
                             GithubClient githubClient) {
        this.userRepository = userRepository;
        this.repositoryRepository = repositoryRepository;
        this.githubClient = githubClient;
    }

    @Transactional
    public void saveSelectedRepositories(
            RepositorySelectionRequest request,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2User oauthUser
    ) {
        // 1. Find authenticated user using githubId from OAuth2User.
        Map<String, Object> attributes = oauthUser.getAttributes();
        Long githubId = ((Number) attributes.get("id")).longValue();
        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with githubId: " + githubId));

        // 2. Get access token from OAuth2AuthorizedClient.
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        // 3. Fetch existing repositories for the authenticated user only once.
        List<Repository> existingRepos = repositoryRepository.findByUser(user);
        for (Repository repo : existingRepos) {
            repo.setSelected(false);
        }

        // 4. Convert them into a Map<Long, Repository> where the key is githubRepoId.
        Map<Long, Repository> repoMap = existingRepos.stream()
                .collect(Collectors.toMap(Repository::getGithubRepoId, repo -> repo));

        // 5. Fetch all repositories from GitHub using GithubClient.
        List<GithubRepositoryResponse> githubRepos = githubClient.getRepositories(accessToken);

        // 6. Replace List.contains() with a HashSet.
        Set<Long> selectedIds = new HashSet<>(request.getGithubRepositoryIds() != null ? request.getGithubRepositoryIds() : List.of());

        // 7. Filter only repositories whose githubRepositoryIds are present in RepositorySelectionRequest.
        List<GithubRepositoryResponse> selectedRepos = githubRepos.stream()
                .filter(repo -> selectedIds.contains(repo.getId()))
                .toList();

        List<Repository> reposToSave = new ArrayList<>(existingRepos);

        // 8. For each selected repository:
        for (GithubRepositoryResponse repoDto : selectedRepos) {
            Repository repository = repoMap.get(repoDto.getId());

            if (repository != null) {
                // If githubRepoId already exists update name, defaultBranch, htmlUrl, privateRepo, updatedAt, selected=true
                repository.setName(repoDto.getName());
                repository.setDefaultBranch(repoDto.getDefaultBranch());
                repository.setHtmlUrl(repoDto.getHtmlUrl());
                repository.setPrivateRepo(repoDto.getIsPrivate());
                repository.setSelected(true);
            } else {
                // Otherwise create a new Repository.
                repository = new Repository();
                repository.setGithubRepoId(repoDto.getId());
                repository.setName(repoDto.getName());
                repository.setDefaultBranch(repoDto.getDefaultBranch());
                repository.setHtmlUrl(repoDto.getHtmlUrl());
                repository.setPrivateRepo(repoDto.getIsPrivate());
                repository.setSelected(true);
                repository.setUser(user);
                reposToSave.add(repository);
            }
        }

        // 9. Save all repositories at once
        repositoryRepository.saveAll(reposToSave);
    }
}
