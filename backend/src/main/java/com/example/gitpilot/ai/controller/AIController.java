package com.example.gitpilot.ai.controller;

import com.example.gitpilot.ai.dto.AIHealthResponse;
import com.example.gitpilot.ai.dto.AIRecommendationsResponse;
import com.example.gitpilot.ai.dto.AISummaryResponse;
import com.example.gitpilot.ai.service.AIService;
import com.example.gitpilot.analysis.dto.RepositoryIntelligenceResponse;
import com.example.gitpilot.analysis.service.RepositoryIntelligenceService;
import com.example.gitpilot.security.GitHubTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "AI Engineering Intelligence Platform")
@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final RepositoryIntelligenceService repositoryIntelligenceService;
    private final GitHubTokenResolver tokenResolver;

    @GetMapping({"/ai/repositories/{id}/intelligence", "/repositories/{id}/intelligence"})
    public ResponseEntity<RepositoryIntelligenceResponse> getIntelligence(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force,
            Authentication authentication, HttpServletRequest request) {

        OAuth2AuthorizedClient client = tokenResolver.getClient(authentication, request);
        RepositoryIntelligenceResponse response = repositoryIntelligenceService.getRepositoryIntelligence(id, client, force);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/ai/repositories/{id}/intelligence/refresh", "/repositories/{id}/intelligence/analyze", "/ai/repositories/{id}/intelligence/analyze", "/repositories/{id}/intelligence/refresh"})
    public ResponseEntity<RepositoryIntelligenceResponse> refreshIntelligence(
            @PathVariable Long id, Authentication authentication, HttpServletRequest request) {

        OAuth2AuthorizedClient client = tokenResolver.getClient(authentication, request);
        RepositoryIntelligenceResponse response = repositoryIntelligenceService.getRepositoryIntelligence(id, client, true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ai/repositories/{id}/health")
    public ResponseEntity<AIHealthResponse> getHealth(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getHealth(id));
    }

    @GetMapping("/ai/repositories/{id}/summary")
    public ResponseEntity<AISummaryResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getSummary(id));
    }

    @GetMapping("/ai/repositories/{id}/recommendations")
    public ResponseEntity<AIRecommendationsResponse> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getRecommendations(id));
    }
}
