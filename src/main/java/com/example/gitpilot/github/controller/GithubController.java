package com.example.gitpilot.github.controller;

import com.example.gitpilot.github.service.GithubService;
import com.example.gitpilot.repository.dto.RepositoryResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/github")
public class GithubController {
    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @GetMapping("/repositories")
    public List<RepositoryResponse> getRepositories(
            @RegisteredOAuth2AuthorizedClient("github")
            OAuth2AuthorizedClient authorizedClient) {

        return githubService.getRepositories(authorizedClient);

    }
}
