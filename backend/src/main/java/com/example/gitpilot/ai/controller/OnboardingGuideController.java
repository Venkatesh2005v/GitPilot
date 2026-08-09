package com.example.gitpilot.ai.controller;

import com.example.gitpilot.ai.dto.OnboardingReportDto;
import com.example.gitpilot.ai.service.OnboardingGuideService;
import com.example.gitpilot.security.GitHubTokenResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Onboarding Engine")
@RestController
@RequiredArgsConstructor
public class OnboardingGuideController {

    private final OnboardingGuideService onboardingGuideService;
    private final GitHubTokenResolver tokenResolver;

    @GetMapping({"/api/ai/onboarding/{repositoryId}", "/ai/onboarding/{repositoryId}"})
    public ResponseEntity<OnboardingReportDto> getOnboardingReport(
            @PathVariable Long repositoryId, Authentication authentication, HttpServletRequest request) {
        String token = tokenResolver.resolve(authentication, request);
        OnboardingReportDto report = onboardingGuideService.getOrGenerateOnboardingGuide(repositoryId, false, token);
        return ResponseEntity.ok(report);
    }

    @PostMapping({"/api/ai/onboarding/{repositoryId}/regenerate", "/ai/onboarding/{repositoryId}/regenerate"})
    public ResponseEntity<OnboardingReportDto> regenerateOnboardingReport(
            @PathVariable Long repositoryId, Authentication authentication, HttpServletRequest request) {
        String token = tokenResolver.resolve(authentication, request);
        OnboardingReportDto report = onboardingGuideService.getOrGenerateOnboardingGuide(repositoryId, true, token);
        return ResponseEntity.ok(report);
    }
}
