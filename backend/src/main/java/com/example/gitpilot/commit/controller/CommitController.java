package com.example.gitpilot.commit.controller;

import com.example.gitpilot.commit.dto.CommitDetailDto;
import com.example.gitpilot.commit.service.CommitDetailService;
import com.example.gitpilot.commit.service.CommitService;
import com.example.gitpilot.security.GitHubTokenResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Commit Synchronization")
@RestController
@RequiredArgsConstructor
public class CommitController {

    private final CommitService commitService;
    private final CommitDetailService commitDetailService;
    private final GitHubTokenResolver tokenResolver;

    @PostMapping({"/repositories/{repositoryId}/sync-commits", "/repositories/{repositoryId}/sync"})
    public ResponseEntity<Map<String, String>> syncCommits(
            @PathVariable("repositoryId") Long repositoryId,
            Authentication authentication, HttpServletRequest request) {

        OAuth2AuthorizedClient authorizedClient = tokenResolver.getClient(authentication, request);
        int count = commitService.syncCommits(repositoryId, authorizedClient);
        return ResponseEntity.ok(Map.of("message", count + " commits synchronized successfully."));
    }

    /**
     * On-demand commit detail: changed files + patches for a single commit.
     * Accepts ONLY repositoryId + sha; owner/repo are resolved server-side from the tracked
     * repository, using the authenticated user's GitHub token. Not a generic GitHub proxy.
     * This multi-segment path is behind authentication via the existing SecurityConfig rules.
     */
    @GetMapping("/repositories/{repositoryId}/commits/{sha}")
    public ResponseEntity<CommitDetailDto> getCommitDetail(
            @PathVariable("repositoryId") Long repositoryId,
            @PathVariable("sha") String sha,
            Authentication authentication, HttpServletRequest request) {

        String accessToken = tokenResolver.resolve(authentication, request);
        CommitDetailDto detail = commitDetailService.getCommitDetail(repositoryId, sha, accessToken);
        return ResponseEntity.ok(detail);
    }
}
