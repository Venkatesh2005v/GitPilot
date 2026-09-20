package com.example.gitpilot.ai.service;

import com.example.gitpilot.ai.dto.OnboardingReportDto;
import com.example.gitpilot.ai.dto.OnboardingReportDto.ModuleDescriptionDto;
import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto;
import com.example.gitpilot.analysis.service.RepositoryEvidenceService;
import com.example.gitpilot.analysis.service.RepositoryEvidenceService.RepositoryEvidence;
import com.example.gitpilot.analysis.service.RepositoryFingerprintService;
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
    // Phase 2 / Phase 1 reuse (do not duplicate detection).
    private final RepositoryFingerprintService fingerprintService;
    private final RepositoryEvidenceService repositoryEvidenceService;
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

        // Parse owner/repo. Prefer htmlUrl (authoritative, matches other analysis services);
        // fall back to a "owner/name" repository name if needed.
        String owner = "";
        String repoShort = "";
        String htmlUrl = repo.getHtmlUrl();
        if (htmlUrl != null && htmlUrl.contains("github.com/")) {
            String path = htmlUrl.substring(htmlUrl.indexOf("github.com/") + 11);
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            String[] parts = path.split("/");
            if (parts.length >= 2) { owner = parts[0]; repoShort = parts[1]; }
        }
        if (owner.isEmpty() && repoName.contains("/")) {
            String[] parts = repoName.split("/", 2);
            owner = parts[0];
            repoShort = parts[1];
        } else if (repoShort.isEmpty()) {
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

        // 7. Reuse Phase 2 deterministic fingerprint + Phase 1 evidence (never duplicate detection).
        //    Both fail safe: unresolved when GitHub/token unavailable, preserving existing behavior.
        RepositoryFingerprintDto fingerprint;
        try {
            fingerprint = fingerprintService.fingerprint(owner, repoShort, accessToken);
        } catch (Exception ex) {
            fingerprint = RepositoryFingerprintDto.builder().resolved(false).build();
        }
        RepositoryEvidence evidence;
        try {
            evidence = repositoryEvidenceService.detect(owner, repoShort, accessToken, configFiles);
        } catch (Exception ex) {
            evidence = RepositoryEvidence.unresolved();
        }

        // Deterministic technology list from the fingerprint (authoritative when resolved).
        List<String> deterministicTech = deterministicTechList(fingerprint);

        // Build the AI prompt with all real data + authoritative fingerprint evidence.
        String prompt = buildOnboardingPrompt(repoName, readme, languages, rootFiles, commitMessages, contributors, configFiles, fingerprint);

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
            // Technology stack: prefer deterministic fingerprint whenever resolved (do not trust AI guesses).
            if (!deterministicTech.isEmpty()) {
                parsed.setTechnologyStack(deterministicTech);
            }
            // First contributions: if AI omitted them, use the evidence-based fallback (never generic filler).
            if (parsed.getSuggestedFirstContributions() == null || parsed.getSuggestedFirstContributions().isEmpty()) {
                parsed.setSuggestedFirstContributions(buildFirstContributionSuggestions(evidence, readme));
            }
            return parsed;
        } catch (Exception e) {
            log.warn("AI onboarding generation failed for {}: {}. Using deterministic fallback.", repoName, e.getMessage());
            return buildDeterministicFallback(repo, languages, rootFiles, commitMessages, contributors, configFiles, fingerprint, evidence, readme);
        }
    }

    /** Human-readable deterministic technology list built from the Phase 2 fingerprint. */
    private List<String> deterministicTechList(RepositoryFingerprintDto fp) {
        List<String> tech = new ArrayList<>();
        if (fp == null || !fp.isResolved()) return tech;
        if (fp.getLanguages() != null) tech.addAll(fp.getLanguages());
        if (fp.getBackendFramework() != null) tech.add(fp.getBackendFramework());
        if (fp.getBackendBuildTool() != null) tech.add(fp.getBackendBuildTool());
        if (fp.getFrontendFramework() != null) tech.add(fp.getFrontendFramework());
        if (fp.getFrontendBuildTool() != null) tech.add(fp.getFrontendBuildTool());
        if (fp.getDatabase() != null) tech.add(fp.getDatabase());
        if (fp.getTestingFrameworks() != null) tech.addAll(fp.getTestingFrameworks());
        if (fp.isDocker()) tech.add("Docker");
        if (fp.isDockerCompose()) tech.add("Docker Compose");
        if (fp.getCiProvider() != null) tech.add(fp.getCiProvider());
        // De-duplicate while preserving order.
        return tech.stream().distinct().toList();
    }

    /**
     * Evidence-based first-contribution suggestions. Only proposes a gap when Phase 1 evidence
     * confirms it is missing; never claims a gap when evidence is unresolved. Returns an honest,
     * non-inventing message when nothing can be safely determined.
     */
    private List<String> buildFirstContributionSuggestions(RepositoryEvidence evidence, String readme) {
        List<String> out = new ArrayList<>();
        boolean resolved = evidence != null && evidence.isResolved();
        if (resolved && !evidence.isHasTests()) {
            out.add("Add automated tests — no test directory or files were detected.");
        }
        if (resolved && !evidence.isHasCI()) {
            out.add("Set up a CI pipeline — no CI configuration (e.g. .github/workflows) was detected.");
        }
        boolean hasReadme = readme != null && readme.trim().length() > 50;
        if (!hasReadme) {
            out.add("Improve project documentation — the repository has little or no README content.");
        }
        if (out.isEmpty()) {
            out.add("Review the repository issues and recent changes to identify a suitable first contribution.");
        }
        return out;
    }

    private String buildOnboardingPrompt(String repoName, String readme, Map<String, Long> languages,
                                          List<String> rootFiles, List<String> commitMessages,
                                          List<ContributorResponse> contributors, List<String> configFiles,
                                          RepositoryFingerprintDto fingerprint) {
        String readmeSnippet = readme.length() > 2000 ? readme.substring(0, 2000) : readme;
        String langStr = languages.isEmpty() ? "Not detected" : languages.keySet().stream().limit(8).collect(Collectors.joining(", "));
        String dirStructure = rootFiles.isEmpty() ? "Not available" : String.join(", ", rootFiles.stream().limit(30).toList());
        String recentCommits = commitMessages.isEmpty() ? "No commits yet" : String.join("\n", commitMessages.stream().limit(10).toList());
        String contribStr = contributors.isEmpty() ? "No contributors yet" :
                contributors.stream().limit(5).map(c -> c.getAuthorName() + " (" + c.getCommitCount() + " commits)").collect(Collectors.joining(", "));
        String fingerprintStr = describeFingerprint(fingerprint);

        return String.format("""
                You are a Senior Developer Onboarding Specialist. Generate a comprehensive onboarding guide for a NEW developer joining this specific repository.
                
                REPOSITORY: %s
                
                README CONTENT:
                %s
                
                GITHUB LANGUAGES (supporting evidence): %s
                
                DETERMINISTIC TECHNOLOGY FINGERPRINT (authoritative — derived from actual build/config files):
                %s
                
                ROOT DIRECTORY STRUCTURE: %s
                
                CONFIG/BUILD FILES: %s
                
                RECENT COMMIT MESSAGES:
                %s
                
                TOP CONTRIBUTORS: %s
                
                Based ONLY on the above information, generate a JSON onboarding guide with this EXACT structure (no markdown, raw JSON only):
                {
                  "projectPurpose": "A 2-3 sentence description of what this project does based on the README and commits",
                  "problemSolved": "The concrete problem this project addresses, derived from README and evidence. If evidence is insufficient, state exactly: 'Insufficient repository evidence to determine the problem solved.'",
                  "primaryRequirements": ["Major capability supported by evidence", "..."],
                  "highLevelDescription": "Architecture and design approach based on the directory structure and technologies",
                  "technologyStack": ["tech1", "tech2", "tech3"],
                  "implementedFeatures": [
                    {"name": "Feature", "description": "What it does", "evidence": "Why it is considered IMPLEMENTED (e.g. controller class, module/dir, dependency, config)"}
                  ],
                  "mainModules": [
                    {"moduleName": "Module Name", "responsibility": "What it does", "keyFiles": ["file1", "file2"]}
                  ],
                  "recommendedLearningPath": ["Step 1: ...", "Step 2: ...", "Step 3: ..."],
                  "importantConfigFiles": ["file1", "file2"],
                  "keyRestApis": ["endpoint description 1", "endpoint description 2"],
                  "suggestedFirstContributions": ["suggestion 1", "suggestion 2", "suggestion 3"]
                }
                
                IMPORTANT RULES:
                - Base EVERYTHING on the actual repository data provided above. Do NOT invent facts.
                - For technologyStack, use the DETERMINISTIC TECHNOLOGY FINGERPRINT as the source of truth. Do NOT add technologies that are not supported by the fingerprint, config files, or languages. You may omit, not contradict.
                - problemSolved: derive from README/evidence only. If there is not enough evidence, say so explicitly rather than guessing the business problem.
                - primaryRequirements: list only MAJOR capabilities supported by evidence. Do NOT convert arbitrary README sentences into confirmed requirements.
                - implementedFeatures: include a feature ONLY when it is backed by concrete code/structure/config/dependency evidence (name the evidence in the 'evidence' field). Something MENTIONED in the README/commits but WITHOUT implementation evidence is NOT implemented — omit it. When unsure, leave implementedFeatures empty rather than over-claiming.
                - If README is empty, infer purpose from commit messages and file structure, and keep implementedFeatures conservative.
                - Keep descriptions concise and actionable.
                """,
                repoName,
                readmeSnippet.isEmpty() ? "(No README available)" : readmeSnippet,
                langStr,
                fingerprintStr,
                dirStructure,
                configFiles.isEmpty() ? "None detected" : String.join(", ", configFiles),
                recentCommits,
                contribStr
        );
    }

    /** Render the fingerprint as authoritative prompt evidence, or note that it is unresolved. */
    private String describeFingerprint(RepositoryFingerprintDto fp) {
        if (fp == null || !fp.isResolved()) {
            return "Not resolved (repository files could not be inspected). Rely on languages/config files/README instead; do not fabricate a stack.";
        }
        StringBuilder sb = new StringBuilder();
        if (fp.getPrimaryLanguage() != null) sb.append("- primaryLanguage: ").append(fp.getPrimaryLanguage()).append("\n");
        if (fp.getLanguages() != null && !fp.getLanguages().isEmpty()) sb.append("- languages: ").append(String.join(", ", fp.getLanguages())).append("\n");
        if (fp.getBackendFramework() != null) sb.append("- backendFramework: ").append(fp.getBackendFramework()).append("\n");
        if (fp.getBackendBuildTool() != null) sb.append("- backendBuildTool: ").append(fp.getBackendBuildTool()).append("\n");
        if (fp.getFrontendFramework() != null) sb.append("- frontendFramework: ").append(fp.getFrontendFramework()).append("\n");
        if (fp.getFrontendBuildTool() != null) sb.append("- frontendBuildTool: ").append(fp.getFrontendBuildTool()).append("\n");
        if (fp.getDatabase() != null) sb.append("- database: ").append(fp.getDatabase()).append("\n");
        if (fp.getTestingFrameworks() != null && !fp.getTestingFrameworks().isEmpty()) sb.append("- testing: ").append(String.join(", ", fp.getTestingFrameworks())).append("\n");
        if (fp.isDocker()) sb.append("- docker: yes\n");
        if (fp.isDockerCompose()) sb.append("- dockerCompose: yes\n");
        if (fp.getCiProvider() != null) sb.append("- ci: ").append(fp.getCiProvider()).append("\n");
        if (fp.getEvidence() != null && !fp.getEvidence().isEmpty()) {
            sb.append("- evidence:\n");
            fp.getEvidence().forEach(e -> sb.append("    * ").append(e.getTechnology())
                    .append(" (").append(e.getStrength()).append(") — ").append(e.getSource()).append("\n"));
        }
        return sb.length() == 0 ? "Resolved but no specific technologies identified." : sb.toString();
    }

    private OnboardingReportDto buildDeterministicFallback(Repository repo, Map<String, Long> languages,
                                                            List<String> rootFiles, List<String> commitMessages,
                                                            List<ContributorResponse> contributors, List<String> configFiles,
                                                            RepositoryFingerprintDto fingerprint, RepositoryEvidence evidence,
                                                            String readme) {
        String repoName = repo.getName() != null ? repo.getName() : "Repository";

        // Determine purpose from commits
        String purpose = commitMessages.isEmpty()
                ? "This repository has no commit history yet. Sync the repository to generate a detailed onboarding guide."
                : "Repository with active development. Primary focus inferred from recent commits: " +
                  commitMessages.stream().limit(3).collect(Collectors.joining("; "));

        // Tech stack: prefer deterministic fingerprint; otherwise fall back to language/root heuristics.
        List<String> techStack = deterministicTechList(fingerprint);
        if (techStack.isEmpty()) {
            techStack = new ArrayList<>(languages.keySet().stream().limit(8).toList());
            if (rootFiles.contains("pom.xml") || rootFiles.stream().anyMatch(f -> f.contains("build.gradle"))) techStack.add("Spring Boot");
            if (rootFiles.contains("package.json")) techStack.add("Node.js");
            if (rootFiles.contains("Dockerfile")) techStack.add("Docker");
        }

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
                .suggestedFirstContributions(buildFirstContributionSuggestions(evidence, readme))
                // Phase 3 foundation fields kept conservative in the deterministic (AI-unavailable) fallback.
                .problemSolved(readme != null && readme.trim().length() > 50
                        ? "See project purpose. A detailed problem statement requires AI analysis; the README is available for reference."
                        : "Insufficient repository evidence to determine the problem solved.")
                .primaryRequirements(List.of())
                .implementedFeatures(List.of())
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
