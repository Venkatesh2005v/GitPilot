package com.example.gitpilot.commit.controller;

import com.example.gitpilot.commit.service.CommitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Commit Synchronization", description = "Endpoints for synchronizing commits from GitHub to local database")
@RestController
public class CommitController {

    private final CommitService commitService;

    public CommitController(CommitService commitService) {
        this.commitService = commitService;
    }

    @Operation(summary = "Sync repository commits", description = "Fetches and persists commit history for a selected repository from GitHub API")
    @PostMapping("/repositories/{repositoryId}/sync-commits")
    public ResponseEntity<Map<String, String>> syncCommits(
            @PathVariable Long repositoryId,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient
    ) {
        int count = commitService.syncCommits(repositoryId, authorizedClient);
        String message = count + " commits synchronized successfully.";
        return ResponseEntity.ok(Map.of("message", message));
    }
}
