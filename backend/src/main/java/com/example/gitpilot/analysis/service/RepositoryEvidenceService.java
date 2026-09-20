package com.example.gitpilot.analysis.service;

import com.example.gitpilot.github.client.GithubClient;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Detects concrete, deterministic repository evidence (tests, CI, Docker) by inspecting
 * the repository's actual file/directory listing via the existing GitHub content APIs.
 *
 * This intentionally does NOT read file contents or build a generic rule engine — it only
 * looks for well-known conventional paths/markers. All lookups fail safe (never throw): if
 * GitHub is unavailable or the token is missing, evidence flags are simply left false and
 * {@link RepositoryEvidence#isResolved()} is false so callers can decide how much to trust it.
 */
@Slf4j
@Service
public class RepositoryEvidenceService {

    private final GithubClient githubClient;

    // Root-level directories that conventionally hold tests.
    private static final Set<String> TEST_DIR_NAMES = Set.of(
            "test", "tests", "__tests__", "spec", "specs"
    );

    // Common language/framework source roots that contain nested test directories.
    private static final List<String> NESTED_TEST_PATHS = List.of(
            "src/test",            // Maven/Gradle (Java/Kotlin)
            "src/test/java",
            "src/test/kotlin",
            "src/test/resources",
            "src/__tests__",       // JS/TS
            "src/tests",
            "app/src/test"         // Android/Gradle modules
    );

    // Well-known monorepo subproject roots, probed first.
    private static final List<String> SUBPROJECT_ROOTS = List.of(
            "backend", "frontend", "server", "client", "api", "web", "app"
    );

    // Upper bound on how many root-level directories we probe (bounds GitHub API calls).
    private static final int MAX_SUBDIR_PROBES = 12;

    // Root entries that are clearly not subproject directories worth probing.
    private static final Set<String> NON_PROJECT_DIRS = Set.of(
            ".git", ".github", ".idea", ".vscode", "node_modules", "target", "dist",
            "build", "docs", "doc", "screenshots", "assets", ".mvn"
    );

    public RepositoryEvidenceService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    @Getter
    @Builder
    public static class RepositoryEvidence {
        /** True once we successfully read at least the repository root listing. */
        private final boolean resolved;
        private final boolean hasTests;
        private final boolean hasCI;
        private final boolean hasDocker;

        public static RepositoryEvidence unresolved() {
            return RepositoryEvidence.builder()
                    .resolved(false).hasTests(false).hasCI(false).hasDocker(false)
                    .build();
        }
    }

    /**
     * Detect evidence for the given owner/repo. Never throws.
     *
     * @param manifestFiles manifest paths already discovered by TechnologyDetectionService
     *                      (e.g. "Dockerfile", "backend/pom.xml"), used as supporting evidence.
     */
    public RepositoryEvidence detect(String owner, String repo, String accessToken, List<String> manifestFiles) {
        if (owner == null || owner.isBlank() || repo == null || repo.isBlank() || accessToken == null) {
            log.debug("[Evidence] Skipped detection (owner/repo/token missing) for {}/{}", owner, repo);
            return RepositoryEvidence.unresolved();
        }

        List<String> rootFiles;
        try {
            rootFiles = githubClient.getRepoContents(owner, repo, accessToken);
        } catch (Exception e) {
            log.debug("[Evidence] Root listing failed for {}/{}: {}", owner, repo, e.getMessage());
            return RepositoryEvidence.unresolved();
        }
        if (rootFiles == null || rootFiles.isEmpty()) {
            // Could be genuinely empty or an API failure; treat as unresolved to avoid false negatives.
            return RepositoryEvidence.unresolved();
        }

        List<String> manifests = manifestFiles != null ? manifestFiles : List.of();

        boolean hasDocker = detectDocker(rootFiles, manifests, owner, repo, accessToken);
        boolean hasCI = detectCI(rootFiles, owner, repo, accessToken);
        boolean hasTests = detectTests(rootFiles, owner, repo, accessToken);

        log.info("[Evidence] {}/{} resolved: hasTests={} hasCI={} hasDocker={}", owner, repo, hasTests, hasCI, hasDocker);

        return RepositoryEvidence.builder()
                .resolved(true)
                .hasTests(hasTests)
                .hasCI(hasCI)
                .hasDocker(hasDocker)
                .build();
    }

    private boolean detectDocker(List<String> rootFiles, List<String> manifests, String owner, String repo, String token) {
        if (containsIgnoreCase(rootFiles, "Dockerfile")
                || rootFiles.stream().anyMatch(f -> f.toLowerCase().startsWith("docker-compose"))) {
            return true;
        }
        if (manifests.stream().anyMatch(m -> {
            String l = m.toLowerCase();
            return l.endsWith("dockerfile") || l.contains("docker-compose");
        })) {
            return true;
        }
        // Probe candidate subproject directories for a Dockerfile.
        for (String sub : candidateSubdirs(rootFiles)) {
            List<String> subFiles = safeList(owner, repo, sub, token);
            if (containsIgnoreCase(subFiles, "Dockerfile")
                    || subFiles.stream().anyMatch(f -> f.toLowerCase().startsWith("docker-compose"))) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCI(List<String> rootFiles, String owner, String repo, String token) {
        // GitLab / Travis / CircleCI / Azure markers at root.
        if (rootFiles.stream().anyMatch(f -> {
            String l = f.toLowerCase();
            return l.equals(".gitlab-ci.yml") || l.equals(".travis.yml")
                    || l.equals("azure-pipelines.yml") || l.equalsIgnoreCase("Jenkinsfile")
                    || l.equals("bitbucket-pipelines.yml");
        })) {
            return true;
        }
        // .circleci/ directory
        if (containsIgnoreCase(rootFiles, ".circleci")) {
            return true;
        }
        // GitHub Actions: .github/workflows/*.yml
        if (containsIgnoreCase(rootFiles, ".github")) {
            List<String> githubDir = safeList(owner, repo, ".github", token);
            if (containsIgnoreCase(githubDir, "workflows")) {
                List<String> workflows = safeList(owner, repo, ".github/workflows", token);
                boolean hasWorkflowFile = workflows.stream().anyMatch(f -> {
                    String l = f.toLowerCase();
                    return l.endsWith(".yml") || l.endsWith(".yaml");
                });
                if (hasWorkflowFile) return true;
            }
        }
        return false;
    }

    private boolean detectTests(List<String> rootFiles, String owner, String repo, String token) {
        // Root-level test directories.
        if (rootFiles.stream().anyMatch(f -> TEST_DIR_NAMES.contains(f.toLowerCase()))) {
            return true;
        }
        // Root-level test files (e.g. foo.test.ts, bar.spec.js, conftest.py).
        if (rootFiles.stream().anyMatch(this::looksLikeTestFile)) {
            return true;
        }
        // Nested conventional test paths.
        for (String path : NESTED_TEST_PATHS) {
            if (!safeList(owner, repo, path, token).isEmpty()) {
                return true;
            }
        }
        // Monorepo subprojects (arbitrary names): probe each candidate root directory for its own
        // test folders / src/test / __tests__. Not limited to a fixed name allowlist so subprojects
        // like "vehicle-scheduler-be" or "notification-app-be" are covered.
        for (String sub : candidateSubdirs(rootFiles)) {
            List<String> subFiles = safeList(owner, repo, sub, token);
            if (subFiles.stream().anyMatch(f -> TEST_DIR_NAMES.contains(f.toLowerCase()))
                    || subFiles.stream().anyMatch(this::looksLikeTestFile)) {
                return true;
            }
            if (!safeList(owner, repo, sub + "/src/test", token).isEmpty()
                    || !safeList(owner, repo, sub + "/src/__tests__", token).isEmpty()
                    || !safeList(owner, repo, sub + "/tests", token).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Derive candidate subproject directory names from the root listing. Root entries with a file
     * extension (e.g. README.md, pom.xml) or known non-project dirs are excluded. Well-known
     * subproject roots are probed first, and the total is capped to bound API calls.
     */
    private List<String> candidateSubdirs(List<String> rootFiles) {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        // Prefer conventional names that are actually present.
        for (String known : SUBPROJECT_ROOTS) {
            if (containsIgnoreCase(rootFiles, known)) ordered.add(matchCase(rootFiles, known));
        }
        // Then any other plausible directory (no dot in the name, not an excluded dir).
        for (String entry : rootFiles) {
            if (entry == null) continue;
            String lower = entry.toLowerCase();
            if (entry.contains(".")) continue;               // has an extension → treat as a file
            if (NON_PROJECT_DIRS.contains(lower)) continue;
            ordered.add(entry);
        }
        return ordered.stream().limit(MAX_SUBDIR_PROBES).toList();
    }

    private String matchCase(List<String> files, String target) {
        return files.stream().filter(f -> f != null && f.equalsIgnoreCase(target)).findFirst().orElse(target);
    }

    private boolean looksLikeTestFile(String name) {
        if (name == null) return false;
        String l = name.toLowerCase();
        return l.endsWith(".test.js") || l.endsWith(".test.jsx")
                || l.endsWith(".test.ts") || l.endsWith(".test.tsx")
                || l.endsWith(".spec.js") || l.endsWith(".spec.jsx")
                || l.endsWith(".spec.ts") || l.endsWith(".spec.tsx")
                || l.equals("conftest.py")
                || (l.startsWith("test_") && l.endsWith(".py"))
                || (l.endsWith("_test.py"))
                || (l.endsWith("_test.go"));
    }

    private List<String> safeList(String owner, String repo, String path, String token) {
        try {
            List<String> result = githubClient.getRepoContentsAtPath(owner, repo, path, token);
            return result != null ? result : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean containsIgnoreCase(List<String> files, String target) {
        if (files == null) return false;
        return files.stream().anyMatch(f -> f != null && f.equalsIgnoreCase(target));
    }
}
