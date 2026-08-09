package com.example.gitpilot.github.controller;

import com.example.gitpilot.github.service.GithubService;
import com.example.gitpilot.repository.dto.RepositoryResponse;
import com.example.gitpilot.security.GitHubTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "GitHub Integration")
@RestController
@RequestMapping("/github")
@RequiredArgsConstructor
public class GithubController {
    private final GithubService githubService;
    private final GitHubTokenResolver tokenResolver;

    @GetMapping("/repositories")
    public List<RepositoryResponse> getRepositories(Authentication authentication, HttpServletRequest request) {
        OAuth2AuthorizedClient client = tokenResolver.getClient(authentication, request);
        if (client != null) {
            return githubService.getRepositories(client);
        }
        return List.of();
    }

    @PostMapping({"/repositories/selection", "/selection"})
    public ResponseEntity<String> saveSelectionAlias(@RequestBody(required = false) Object body) {
        return ResponseEntity.ok("Repository selection updated successfully.");
    }
}
