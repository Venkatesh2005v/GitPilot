package com.example.gitpilot.ai.service;

import com.example.gitpilot.ai.dto.OnboardingReportDto;
import com.example.gitpilot.ai.dto.OnboardingReportDto.ModuleDescriptionDto;
import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.dashboard.dto.ContributorResponse;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingGuideService {

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final GithubClient githubClient;
    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache key: repositoryId -> (commitSha, report)
    private final Map<Long, CachedOnboarding> reportCache = new ConcurrentHashMap<>();

    private record CachedOnboarding(String latestCommitSha, OnboardingReportDto report) {}

    @Transactional(readOnly = true)
    public OnboardingReportDto getOrGenerateOnboardingGuide(Long repositoryId, boolean forceRegenerate, String accessToken) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        // Get latest commit SHA for cache key
        Optional<Commit> latestCommit = commitRepository.findFirstByRepositoryOrderByCommitDateDesc(repository);
        String latestSha = latestCommit.map(Commit::getGithubCommitSha).orElse("no-commits");

        // Check cache (only valid if same commit SHA)
        if (!forceRegenerate) {
            CachedOnboarding cached = reportCache.get(repositoryId);
            if (cached != null && cached.latestCommitSha().equals(latestSha)) {
                OnboardingReportDto report = cached.report();
                report.setCached(true);
                return report;
            }
        }

        // Generate fresh report with real repository data
        OnboardingReportDto report = generateRepositorySpecificReport(repository, latestCommit.orElse(null), accessToken);
        reportCache.put(repositoryId, new CachedOnboarding(latestSha, report));
        return report;
    }

    public void evictCache(Long repositoryId) {
        reportCache.remove(repositoryId);
        log.info("Evicted onboarding cache for repository {}", repositoryId);
    }

    private OnboardingReportDto generateRepositorySpecificReport(Repository repo, Commit latestCommit, String accessToken) {
        String repoName = repo.getName() != null ? repo.getName() : "Unknown Repository";

        // Parse owner/repo
        String owner = "";
        String repoShort = "";
        if (repoName.contains("/")) {
            String[] parts = repoName.split("/", 2);
            owner = parts[0];
            repoShort = parts[1];
        } else {
            repoShort = repoName;
        }

        // 1. Fetch README
        String readme = "";
        if (!owner.isEmpty() && accessToken != null) {
            readme = githubClient.getReadme(owner, repoShort, accessToken);
        }

        // 2. Fetch languages
        Map<String, Long> languages = Map.of();
        if (!owner.isEmpty() && accessToken != null) {
            languages = githubClient.getLanguages(owner, repoShort, accessToken);
        }

        // 3. Fetch root directory structure
        List<String> rootFiles = List.of();
        if (!owner.isEmpty() && accessToken != null) {
            rootFiles = githubClient.getRepoContents(owner, repoShort, accessToken);
        }

        // 4. Get recent commits from DB
        List<Commit> recentCommits = commitRepository.findByRepositoryOrderByCommitDateDesc(repo);
        List<String> commitMessages = recentCommits.stream()
                .limit(20)
                .map(Commit::getMessage)
                .filter(Objects::nonNull)
                .toList();

        // 5. Get contributor statistics from DB
        List<ContributorResponse> contributors = commitRepository.findContributorsByRepository(repo);

        // 6. Detect build/config files
        List<String> configFiles = rootFiles.stream()
                .filter(f -> f.matches("(?i)(pom\\.xml|build\\.gradle.*|package\\.json|Dockerfile|docker-compose.*|Makefile|Cargo\\.toml|go\\.mod|requirements\\.txt|pyproject\\.toml|\\.env\\.example|tsconfig\\.json|vite\\.config\\..*)"))
                .toList();

        // Build the AI prompt with all real data
        String prompt = buildOnboardingPrompt(repoName, readme, languages, rootFiles, commitMessages, contributors, configFiles);

        try {
            AIGatewayService.AIResult aiResult = aiGatewayService.generateInsightWithFailover(prompt);
            String rawJson = cleanJsonResponse(aiResult.text);
            OnboardingReportDto parsed = objectMapper.readValue(rawJson, OnboardingReportDto.class);
            parsed.setRepositoryId(repo.getId());
            parsed.setRepositoryName(repoName);
            parsed.setGeneratedAt(LocalDateTime.now());
            parsed.setCached(false);
            // Ensure config files reflect actual data
            if (parsed.getImportantConfigFiles() == null || parsed.getImportantConfigFiles().isEmpty()) {
                parsed.setImportantConfigFiles(configFiles);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("AI onboarding generation failed for {}: {}. Using deterministic fallback.", repoName, e.getMessage());
            return buildDeterministicFallback(repo, languages, rootFiles, commitMessages, contributors, configFiles);
        }
    }

    private String buildOnboardingPrompt(String repoName, String readme, Map<String, Long> languages,
                                          List<String> rootFiles, List<String> commitMessages,
                                          List<ContributorResponse> contributors, List<String> configFiles) {
        String readmeSnippet = readme.length() > 2000 ? readme.substring(0, 2000) : readme;
        String langStr = languages.isEmpty() ? "Not detected" : languages.keySet().stream().limit(8).collect(Collectors.joining(", "));
        String dirStructure = rootFiles.isEmpty() ? "Not available" : String.join(", ", rootFiles.stream().limit(30).toList());
        String recentCommits = commitMessages.isEmpty() ? "No commits yet" : String.join("\n", commitMessages.stream().limit(10).toList());
        String contribStr = contributors.isEmpty() ? "No contributors yet" :
                contributors.stream().limit(5).map(c -> c.getAuthorName() + " (" + c.getCommitCount() + " commits)").collect(Collectors.joining(", "));

        return String.format("""
                You are a Senior Developer Onboarding Specialist. Generate a comprehensive onboarding guide for a NEW developer joining this specific repository.
                
                REPOSITORY: %s
                
                README CONTENT:
                %s
                
                LANGUAGES: %s
                
                ROOT DIRECTORY STRUCTURE: %s
                
                CONFIG/BUILD FILES: %s
                
                RECENT COMMIT MESSAGES:
                %s
                
                TOP CONTRIBUTORS: %s
                
                Based ONLY on the above information, generate a JSON onboarding guide with this EXACT structure (no markdown, raw JSON only):
                {
                  "projectPurpose": "A 2-3 sentence description of what this project does based on the README and commits",
                  "highLevelDescription": "Architecture and design approach based on the directory structure and technologies",
                  "technologyStack": ["tech1", "tech2", "tech3"],
                  "mainModules": [
                    {"moduleName": "Module Name", "responsibility": "What it does", "keyFiles": ["file1", "file2"]}
                  ],
                  "recommendedLearningPath": ["Step 1: ...", "Step 2: ...", "Step 3: ..."],
                  "importantConfigFiles": ["file1", "file2"],
                  "keyRestApis": ["endpoint description 1", "endpoint description 2"],
                  "suggestedFirstContributions": ["suggestion 1", "suggestion 2", "suggestion 3"]
                }
                
                IMPORTANT RULES:
                - Base EVERYTHING on the actual repository data provided above.
                - Do NOT make assumptions about technologies not present in the data.
                - If README is empty, infer purpose from commit messages and file structure.
                - Keep descriptions concise and actionable.
                """,
                repoName,
                readmeSnippet.isEmpty() ? "(No README available)" : readmeSnippet,
                langStr,
                dirStructure,
                configFiles.isEmpty() ? "None detected" : String.join(", ", configFiles),
                recentCommits,
                contribStr
        );
    }

    private OnboardingReportDto buildDeterministicFallback(Repository repo, Map<String, Long> languages,
                                                            List<String> rootFiles, List<String> commitMessages,
                                                            List<ContributorResponse> contributors, List<String> configFiles) {
        String repoName = repo.getName() != null ? repo.getName() : "Repository";

        // Determine purpose from commits
        String purpose = commitMessages.isEmpty()
                ? "This repository has no commit history yet. Sync the repository to generate a detailed onboarding guide."
                : "Repository with active development. Primary focus inferred from recent commits: " +
                  commitMessages.stream().limit(3).collect(Collectors.joining("; "));

        // Tech stack from languages
        List<String> techStack = new ArrayList<>(languages.keySet().stream().limit(8).toList());
        if (rootFiles.contains("pom.xml") || rootFiles.stream().anyMatch(f -> f.contains("build.gradle"))) techStack.add("Spring Boot");
        if (rootFiles.contains("package.json")) techStack.add("Node.js");
        if (rootFiles.contains("Dockerfile")) techStack.add("Docker");

        // Modules from directory structure
        List<ModuleDescriptionDto> modules = rootFiles.stream()
                .filter(f -> !f.contains(".") || f.endsWith("/"))
                .limit(6)
                .map(dir -> ModuleDescriptionDto.builder()
                        .moduleName(dir)
                        .responsibility("Project directory - explore to understand its role")
                        .keyFiles(List.of())
                        .build())
                .toList();

        // Learning path
        List<String> learningPath = new ArrayList<>();
        learningPath.add("1. Read the README and project documentation");
        if (!configFiles.isEmpty()) learningPath.add("2. Review configuration files: " + String.join(", ", configFiles.stream().limit(3).toList()));
        learningPath.add(learningPath.size() + 1 + ". Explore the directory structure and main entry points");
        learningPath.add(learningPath.size() + 1 + ". Read recent commit messages to understand current development focus");
        if (!contributors.isEmpty()) {
            learningPath.add(learningPath.size() + 1 + ". Reach out to top contributor: " + contributors.get(0).getAuthorName());
        }

        return OnboardingReportDto.builder()
                .repositoryId(repo.getId())
                .repositoryName(repoName)
                .projectPurpose(purpose)
                .highLevelDescription("Deterministic analysis based on repository metadata. Regenerate with AI for a deeper guide.")
                .technologyStack(techStack)
                .mainModules(modules.isEmpty() ? List.of() : modules)
                .recommendedLearningPath(learningPath)
                .importantConfigFiles(configFiles)
                .keyRestApis(List.of())
                .suggestedFirstContributions(List.of(
                        "Improve documentation or add a README if missing",
                        "Add or expand test coverage",
                        "Review and fix any open issues"
                ))
                .generatedAt(LocalDateTime.now())
                .isCached(false)
                .build();
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```")) trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        return trimmed.trim();
    }
}
