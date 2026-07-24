package com.example.gitpilot.github.controller;

import com.example.gitpilot.github.service.GithubService;
import com.example.gitpilot.repository.dto.RepositoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "GitHub Integration", description = "Endpoints for fetching data directly from GitHub API")
@RestController
@RequestMapping("/github")
public class GithubController {
    private final GithubService githubService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GithubController(GithubService githubService, OAuth2AuthorizedClientService authorizedClientService) {
        this.githubService = githubService;
        this.authorizedClientService = authorizedClientService;
    }

    @Operation(summary = "Get user repositories from GitHub", description = "Fetches and maps all repositories of the authenticated user directly from GitHub API")
    @GetMapping("/repositories")
    public List<RepositoryResponse> getRepositories(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );
            if (authorizedClient != null) {
                return githubService.getRepositories(authorizedClient);
            }
        }

        // Return sandbox mock repositories for offline/recruiter sandbox evaluation
        return List.of(
            new RepositoryResponse(11111111L, "mock-react-dashboard", "main", "https://github.com/mock-user/mock-react-dashboard", false, true),
            new RepositoryResponse(22222222L, "mock-spring-api", "main", "https://github.com/mock-user/mock-spring-api", false, true),
            new RepositoryResponse(33333333L, "mock-data-pipeline", "main", "https://github.com/mock-user/mock-data-pipeline", true, false)
        );
    }
}
