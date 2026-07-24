package com.example.gitpilot.commit.controller;

import com.example.gitpilot.commit.service.CommitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Commit Synchronization", description = "Endpoints for synchronizing commits from GitHub to local database")
@RestController
public class CommitController {

    private final CommitService commitService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public CommitController(CommitService commitService, OAuth2AuthorizedClientService authorizedClientService) {
        this.commitService = commitService;
        this.authorizedClientService = authorizedClientService;
    }

    @Operation(summary = "Sync repository commits", description = "Fetches and persists commit history for a selected repository from GitHub API")
    @PostMapping({"/repositories/{repositoryId}/sync-commits", "/repositories/{repositoryId}/sync"})
    public ResponseEntity<Map<String, String>> syncCommits(
            @PathVariable("repositoryId") Long repositoryId,
            Authentication authentication
    ) {
        OAuth2AuthorizedClient authorizedClient = null;
        if (authentication instanceof OAuth2AuthenticationToken token) {
            authorizedClient = authorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );
        }

        int count = commitService.syncCommits(repositoryId, authorizedClient);
        String message = count + " commits synchronized successfully.";
        return ResponseEntity.ok(Map.of("message", message));
    }
}
