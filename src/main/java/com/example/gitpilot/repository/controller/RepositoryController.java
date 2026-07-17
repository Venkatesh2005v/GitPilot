package com.example.gitpilot.repository.controller;

import com.example.gitpilot.repository.dto.RepositorySelectionRequest;
import com.example.gitpilot.repository.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Repository Management", description = "Endpoints for managing repository selections")
@RestController
@RequestMapping("/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService){
        this.repositoryService = repositoryService;
    }

    @Operation(summary = "Save repository selections", description = "Saves or updates user's repository selection preferences in the database")
    @PostMapping("/select")
    public ResponseEntity<String> selectRepositories(
            @Valid @RequestBody RepositorySelectionRequest request,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OAuth2User user) {

        repositoryService.saveSelectedRepositories(request, authorizedClient, user);
        return ResponseEntity.ok("Repository selection saved successfully.");
    }

}