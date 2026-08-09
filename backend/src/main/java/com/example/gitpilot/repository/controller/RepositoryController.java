package com.example.gitpilot.repository.controller;

import com.example.gitpilot.repository.dto.RepositorySelectionRequest;
import com.example.gitpilot.repository.service.RepositoryService;
import com.example.gitpilot.security.GitHubTokenResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Repository Management")
@RestController
@RequestMapping("/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final GitHubTokenResolver tokenResolver;

    @PostMapping({"/select", "/selection"})
    public ResponseEntity<String> selectRepositories(
            @Valid @RequestBody RepositorySelectionRequest request,
            Authentication authentication,
            @AuthenticationPrincipal OAuth2User user,
            HttpServletRequest httpRequest) {

        OAuth2AuthorizedClient authorizedClient = tokenResolver.getClient(authentication, httpRequest);
        repositoryService.saveSelectedRepositories(request, authorizedClient, user);
        return ResponseEntity.ok("Repository selection saved successfully.");
    }
}
