package com.example.gitpilot.memory.service;

import com.example.gitpilot.memory.dto.OnboardingStepDto;
import com.example.gitpilot.memory.entity.OnboardingStep;
import com.example.gitpilot.memory.repository.OnboardingStepRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    private final RepositoryRepository repositoryRepository;
    private final OnboardingStepRepository onboardingStepRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnboardingService(RepositoryRepository repositoryRepository,
                             OnboardingStepRepository onboardingStepRepository) {
        this.repositoryRepository = repositoryRepository;
        this.onboardingStepRepository = onboardingStepRepository;
    }

    @Transactional
    public List<OnboardingStepDto> getOnboardingRoadmap(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        List<OnboardingStep> steps = onboardingStepRepository.findByRepositoryOrderByStepOrderAsc(repository);
        if (steps.isEmpty()) {
            steps = seedOnboardingSteps(repository);
        }

        return steps.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<OnboardingStep> seedOnboardingSteps(Repository repository) {
        List<OnboardingStep> steps = new ArrayList<>();

        steps.add(createStep(repository, 1, "Start Here", "Architecture & Repository Overview",
                "Understand project entry points, Spring Boot application entry class, Flyway migrations, and React Vite configuration.",
                5, "EASY", List.of("None"), List.of("GitpilotApplication.java", "vite.config.js", "application.yml")));

        steps.add(createStep(repository, 2, "Authentication", "OAuth2 & Security Flow",
                "Learn how GitHub OAuth2 login flow works, SecurityConfig filter chain, and session user synchronization.",
                10, "MEDIUM", List.of("Start Here"), List.of("SecurityConfig.java", "AuthController.java", "UserService.java")));

        steps.add(createStep(repository, 3, "Repository Synchronization", "GitHub Integration API",
                "Understand how repositories and commits are synchronized from GitHub REST API into PostgreSQL storage.",
                15, "MEDIUM", List.of("Authentication"), List.of("RepositoryController.java", "GithubService.java", "CommitService.java")));

        steps.add(createStep(repository, 4, "Webhook Processing", "Real-Time Push Event Pipeline",
                "Study HMAC signature verification, strategy-pattern handlers, and push event processing for zero-delay indexing.",
                20, "INTERMEDIATE", List.of("Repository Synchronization"), List.of("WebhookController.java", "GithubWebhookHandler.java", "PushWebhookHandler.java")));

        steps.add(createStep(repository, 5, "Insights Engine", "AI Repository Memory Platform", "Explore how AI Reports, Repository DNA, Engineering Timelines, and Knowledge Maps are synthesized and cached.", 25, "ADVANCED", List.of("Webhook Processing"), List.of("RepositoryIntelligenceService.java", "RepositoryDNAService.java", "KnowledgeGraphService.java")));

        steps.add(createStep(repository, 6, "Notification Module", "Async Real-Time Event Dispatch", "Learn how real-time WebSocket / SSE alerts broadcast commit events and AI recommendations to connected developers.", 15, "MEDIUM", List.of("Insights Engine"), List.of("NotificationService.java", "NotificationController.java")));

        return onboardingStepRepository.saveAll(steps);
    }

    private OnboardingStep createStep(Repository repo, int order, String module, String title, String why, int time, String diff, List<String> deps, List<String> files) {
        OnboardingStep s = new OnboardingStep();
        s.setRepository(repo);
        s.setStepOrder(order);
        s.setModuleName(module);
        s.setTitle(title);
        s.setWhyItMatters(why);
        s.setReadingTimeMinutes(time);
        s.setDifficulty(diff);
        try {
            s.setDependenciesJson(objectMapper.writeValueAsString(deps));
            s.setKeyFilesJson(objectMapper.writeValueAsString(files));
        } catch (Exception ignored) {}
        return s;
    }

    private OnboardingStepDto toDto(OnboardingStep s) {
        List<String> deps = List.of();
        List<String> files = List.of();
        try {
            if (s.getDependenciesJson() != null) deps = objectMapper.readValue(s.getDependenciesJson(), new TypeReference<List<String>>() {});
            if (s.getKeyFilesJson() != null) files = objectMapper.readValue(s.getKeyFilesJson(), new TypeReference<List<String>>() {});
        } catch (Exception ignored) {}

        return OnboardingStepDto.builder()
                .id(s.getId())
                .repositoryId(s.getRepository().getId())
                .stepOrder(s.getStepOrder())
                .moduleName(s.getModuleName())
                .title(s.getTitle())
                .whyItMatters(s.getWhyItMatters())
                .readingTimeMinutes(s.getReadingTimeMinutes())
                .difficulty(s.getDifficulty())
                .dependencies(deps)
                .keyFiles(files)
                .build();
    }
}
