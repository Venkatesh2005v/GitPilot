package com.example.gitpilot.ai.controller;

import com.example.gitpilot.ai.dto.AIHealthResponse;
import com.example.gitpilot.ai.dto.AIRecommendationsResponse;
import com.example.gitpilot.ai.dto.AISummaryResponse;
import com.example.gitpilot.ai.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Engineering Insights", description = "Endpoints for generating repository metrics insights using AI Gateway")
@RestController
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "Get repository health insights", description = "Returns health score, strengths, and weaknesses for a repository")
    @GetMapping("/ai/repositories/{id}/health")
    public ResponseEntity<AIHealthResponse> getHealth(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getHealth(id));
    }

    @Operation(summary = "Get repository summary", description = "Generates a weekly engineering summary for a repository")
    @GetMapping("/ai/repositories/{id}/summary")
    public ResponseEntity<AISummaryResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getSummary(id));
    }

    @Operation(summary = "Get repository engineering recommendations", description = "Generates actionable recommendations for a repository")
    @GetMapping("/ai/repositories/{id}/recommendations")
    public ResponseEntity<AIRecommendationsResponse> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.getRecommendations(id));
    }
}
