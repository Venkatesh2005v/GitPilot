package com.example.gitpilot.github.service;

import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubRepositoryResponse;
import com.example.gitpilot.repository.dto.RepositoryResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubService {

    private final GithubClient githubClient;
    private final RepositoryRepository repositoryRepository;

    public GithubService(GithubClient githubClient, RepositoryRepository repositoryRepository) {
        this.githubClient = githubClient;
        this.repositoryRepository = repositoryRepository;
    }

    public List<RepositoryResponse> getRepositories(OAuth2AuthorizedClient authorizedClient) {
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        List<GithubRepositoryResponse> githubRepos = githubClient.getRepositories(accessToken);

        return githubRepos.stream()
                .map(repo -> {
                    boolean selected = repositoryRepository.findByGithubRepoId(repo.getId())
                            .map(Repository::getSelected)
                            .orElse(false);
                    return new RepositoryResponse(
                            repo.getId(),
                            repo.getName(),
                            repo.getDefaultBranch(),
                            repo.getHtmlUrl(),
                            repo.getIsPrivate(),
                            selected
                    );
                })
                .toList();
    }
}
