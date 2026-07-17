package com.example.gitpilot.github.controller;

import com.example.gitpilot.github.service.GithubService;
import com.example.gitpilot.repository.dto.RepositoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "GitHub Integration", description = "Endpoints for fetching data directly from GitHub API")
@RestController
@RequestMapping("/github")
public class GithubController {
    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @Operation(summary = "Get user repositories from GitHub", description = "Fetches and maps all repositories of the authenticated user directly from GitHub API")
    @GetMapping("/repositories")
    public List<RepositoryResponse> getRepositories(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient authorizedClient) {

        return githubService.getRepositories(authorizedClient);

    }
}
