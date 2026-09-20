package com.example.gitpilot.analysis.service;

import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.repository.entity.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TechnologyDetectionService {

    private final GithubClient githubClient;
    private final RepositoryFingerprintService fingerprintService;

    // Manifest file → [language, build tool/framework]
    private static final Map<String, String[]> MANIFEST_MAP = Map.ofEntries(
            Map.entry("pom.xml", new String[]{"Java", "Maven"}),
            Map.entry("build.gradle", new String[]{"Java", "Gradle"}),
            Map.entry("build.gradle.kts", new String[]{"Kotlin", "Gradle"}),
            Map.entry("settings.gradle", new String[]{"Java", "Gradle"}),
            Map.entry("settings.gradle.kts", new String[]{"Kotlin", "Gradle"}),
            Map.entry("package.json", new String[]{"JavaScript", "Node.js"}),
            Map.entry("package-lock.json", new String[]{"JavaScript", "Node.js"}),
            Map.entry("pnpm-lock.yaml", new String[]{"JavaScript", "Node.js"}),
            Map.entry("yarn.lock", new String[]{"JavaScript", "Node.js"}),
            Map.entry("Cargo.toml", new String[]{"Rust", "Cargo"}),
            Map.entry("go.mod", new String[]{"Go", "Go Modules"}),
            Map.entry("composer.json", new String[]{"PHP", "Composer"}),
            Map.entry("requirements.txt", new String[]{"Python", "pip"}),
            Map.entry("pyproject.toml", new String[]{"Python", "Poetry"}),
            Map.entry("Pipfile", new String[]{"Python", "Pipenv"}),
            Map.entry("Gemfile", new String[]{"Ruby", "Bundler"}),
            Map.entry("pubspec.yaml", new String[]{"Dart", "Flutter"}),
            Map.entry("mix.exs", new String[]{"Elixir", "Mix"}),
            Map.entry("CMakeLists.txt", new String[]{"C++", "CMake"}),
            Map.entry("Makefile", new String[]{"C", "Make"}),
            Map.entry("Dockerfile", new String[]{null, "Docker"}),
            Map.entry("docker-compose.yml", new String[]{null, "Docker Compose"}),
            Map.entry("docker-compose.yaml", new String[]{null, "Docker Compose"})
    );

    // Directories that indicate monorepo sub-projects
    private static final Set<String> MONOREPO_DIRS = Set.of(
            "backend", "frontend", "client", "server", "apps", "packages", "services", "api", "web", "app"
    );

    // Names that must NEVER be primaryLanguage
    private static final Set<String> NON_LANGUAGES = Set.of(
            "Spring Boot", "Maven", "Gradle", "Node.js", "Docker", "Docker Compose",
            "React", "Angular", "Vue", "Vite", "Next.js", "NestJS",
            "Flyway", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Kafka",
            "OAuth2", "JWT", "Express", "Django", "FastAPI", "Flask",
            "Hibernate", "JUnit", "Lombok", "Spring Security",
            "Cargo", "Go Modules", "pip", "Poetry", "Pipenv", "Bundler",
            "Flutter", "Mix", "CMake", "Make", "Composer",
            "actix", "rocket", "gin", "fiber"
    );

    public TechnologyDetectionService(GithubClient githubClient, RepositoryFingerprintService fingerprintService) {
        this.githubClient = githubClient;
        this.fingerprintService = fingerprintService;
    }

    public TechStackDto detectTechStack(Repository repository, String accessToken, String readmeContent, List<String> commitMessages) {
        Set<String> detectedTech = new LinkedHashSet<>();
        List<String> manifestFiles = new ArrayList<>();

        // Resolve owner/repo from htmlUrl
        String owner = "";
        String repo = "";
        if (repository.getHtmlUrl() != null && repository.getHtmlUrl().contains("github.com/")) {
            String path = repository.getHtmlUrl().substring(repository.getHtmlUrl().indexOf("github.com/") + 11);
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                owner = parts[0];
                repo = parts[1];
            }
        }
        if (owner.isEmpty() && repository.getName() != null && repository.getName().contains("/")) {
            String[] parts = repository.getName().split("/", 2);
            owner = parts[0];
            repo = parts[1];
        }

        log.info("[TECH] Starting detection for {}/{} token={}", owner, repo, accessToken != null ? "present" : "absent");

        // ===================== PRIORITY 1: GitHub Languages API =====================
        Map<String, Long> languages = Map.of();
        if (!owner.isEmpty() && !repo.isEmpty() && accessToken != null) {
            try {
                languages = githubClient.getLanguages(owner, repo, accessToken);
                log.info("[TECH] P1 Languages API for {}/{}: {}", owner, repo, languages);
            } catch (Exception e) {
                log.error("[TECH] P1 Languages API failed for {}/{}: {}", owner, repo, e.getMessage());
            }
        }
        // Add all languages from API
        for (String lang : languages.keySet()) {
            detectedTech.add(lang);
        }

        // ===================== PRIORITY 2: Manifest/Build Files =====================
        List<String> rootFiles = List.of();
        if (!owner.isEmpty() && !repo.isEmpty() && accessToken != null) {
            try {
                rootFiles = githubClient.getRepoContents(owner, repo, accessToken);
                log.info("[TECH] P2 Root files for {}/{}: {}", owner, repo, rootFiles);
            } catch (Exception e) {
                log.error("[TECH] P2 Root files failed for {}/{}: {}", owner, repo, e.getMessage());
            }
        }

        // Scan root for manifest files
        detectManifests(rootFiles, "", detectedTech, manifestFiles);

        // Monorepo: scan known subdirectories
        for (String rootItem : rootFiles) {
            if (MONOREPO_DIRS.contains(rootItem.toLowerCase()) && accessToken != null) {
                try {
                    List<String> subFiles = githubClient.getRepoContentsAtPath(owner, repo, rootItem, accessToken);
                    log.info("[TECH] P2 Monorepo subdir {}/{}/{}: {}", owner, repo, rootItem, subFiles);
                    detectManifests(subFiles, rootItem + "/", detectedTech, manifestFiles);
                } catch (Exception e) {
                    log.debug("[TECH] Could not read subdir {}: {}", rootItem, e.getMessage());
                }
            }
        }

        // ===================== PRIORITY 3: File Extensions (from root listing) =====================
        // Only used if Languages API returned nothing
        if (languages.isEmpty()) {
            for (String file : rootFiles) {
                String ext = getExtension(file);
                String lang = extensionToLanguage(ext);
                if (lang != null) {
                    detectedTech.add(lang);
                }
            }
        }

        // ===================== PRIORITY 4: README (frameworks/tools only) =====================
        if (readmeContent != null && !readmeContent.isBlank()) {
            String lower = readmeContent.toLowerCase();
            addIfContains(lower, "spring boot", detectedTech, "Spring Boot");
            addIfContains(lower, "spring security", detectedTech, "Spring Security");
            addIfContains(lower, "hibernate", detectedTech, "Hibernate");
            addIfContains(lower, "flyway", detectedTech, "Flyway");
            addIfContains(lower, "react", detectedTech, "React");
            addIfContains(lower, "angular", detectedTech, "Angular");
            addIfContains(lower, "vue", detectedTech, "Vue");
            addIfContains(lower, "next.js", detectedTech, "Next.js");
            addIfContains(lower, "nestjs", detectedTech, "NestJS");
            addIfContains(lower, "express", detectedTech, "Express");
            addIfContains(lower, "django", detectedTech, "Django");
            addIfContains(lower, "fastapi", detectedTech, "FastAPI");
            addIfContains(lower, "flask", detectedTech, "Flask");
            addIfContains(lower, "postgresql", detectedTech, "PostgreSQL");
            addIfContains(lower, "postgres", detectedTech, "PostgreSQL");
            addIfContains(lower, "mysql", detectedTech, "MySQL");
            addIfContains(lower, "mongodb", detectedTech, "MongoDB");
            addIfContains(lower, "redis", detectedTech, "Redis");
            addIfContains(lower, "kafka", detectedTech, "Kafka");
            addIfContains(lower, "docker", detectedTech, "Docker");
        }

        // ===================== PRIORITY 5: Commit messages (enrich only, lowest confidence) =====================
        if (commitMessages != null && !commitMessages.isEmpty()) {
            String commitText = String.join(" ", commitMessages).toLowerCase();
            // Only add tools/frameworks if the language is already detected
            if (detectedTech.stream().anyMatch(t -> t.equals("Java") || t.equals("Kotlin"))) {
                addIfContains(commitText, "flyway", detectedTech, "Flyway");
                addIfContains(commitText, "lombok", detectedTech, "Lombok");
            }
            if (detectedTech.stream().anyMatch(t -> t.equals("JavaScript") || t.equals("TypeScript"))) {
                addIfContains(commitText, "vite", detectedTech, "Vite");
            }
            addIfContains(commitText, "docker", detectedTech, "Docker");
        }

        // ===================== PRIORITY 0: Deterministic Fingerprint (strongest) =====================
        // Inspect actual file CONTENTS. Strong dependency/config evidence overrides weaker heuristics.
        com.example.gitpilot.analysis.dto.RepositoryFingerprintDto fingerprint = null;
        try {
            fingerprint = fingerprintService.fingerprint(owner, repo, accessToken);
        } catch (Exception e) {
            log.warn("[TECH] Fingerprint failed for {}/{}: {}", owner, repo, e.getMessage());
        }

        String fpPrimaryLanguage = null;
        if (fingerprint != null && fingerprint.isResolved()) {
            // Merge fingerprint-detected technologies (strong, content-based evidence) first.
            if (fingerprint.getLanguages() != null) detectedTech.addAll(fingerprint.getLanguages());
            if (fingerprint.getBackendFramework() != null) detectedTech.add(fingerprint.getBackendFramework());
            if (fingerprint.getBackendBuildTool() != null) detectedTech.add(fingerprint.getBackendBuildTool());
            if (fingerprint.getFrontendFramework() != null) detectedTech.add(fingerprint.getFrontendFramework());
            if (fingerprint.getFrontendBuildTool() != null) detectedTech.add(fingerprint.getFrontendBuildTool());
            if (fingerprint.getBackendLibraries() != null) detectedTech.addAll(fingerprint.getBackendLibraries());
            if (fingerprint.getFrontendLibraries() != null) detectedTech.addAll(fingerprint.getFrontendLibraries());
            if (fingerprint.getTestingFrameworks() != null) detectedTech.addAll(fingerprint.getTestingFrameworks());
            if (fingerprint.getDatabase() != null) detectedTech.add(fingerprint.getDatabase());
            if (fingerprint.isDocker()) detectedTech.add("Docker");
            if (fingerprint.isDockerCompose()) detectedTech.add("Docker Compose");
            fpPrimaryLanguage = fingerprint.getPrimaryLanguage();
        }

        // ===================== Determine Primary Language =====================
        // Fingerprint primary language wins when resolved (it is derived from build files/deps).
        String primaryLanguage = (fpPrimaryLanguage != null && !fpPrimaryLanguage.isBlank())
                ? fpPrimaryLanguage
                : determinePrimaryLanguage(languages, manifestFiles, detectedTech);

        // ===================== Determine Category =====================
        String category = determineCategory(detectedTech, primaryLanguage);

        log.info("[TECH] Final: primaryLanguage={} category={} technologies={} manifests={} fingerprintResolved={}",
                primaryLanguage, category, detectedTech, manifestFiles, fingerprint != null && fingerprint.isResolved());

        return TechStackDto.builder()
                .detectedTechnologies(new ArrayList<>(detectedTech))
                .detectedManifestFiles(manifestFiles.stream().distinct().toList())
                .primaryLanguage(primaryLanguage)
                .category(category)
                .fingerprint(fingerprint)
                .build();
    }

    private void detectManifests(List<String> files, String prefix, Set<String> detectedTech, List<String> manifestFiles) {
        for (String file : files) {
            String lower = file.toLowerCase();
            // Check exact matches in manifest map
            for (Map.Entry<String, String[]> entry : MANIFEST_MAP.entrySet()) {
                if (lower.equals(entry.getKey().toLowerCase())) {
                    manifestFiles.add(prefix + file);
                    String[] mapping = entry.getValue();
                    if (mapping[0] != null) detectedTech.add(mapping[0]);  // language
                    if (mapping[1] != null) detectedTech.add(mapping[1]);  // tool/framework
                    break;
                }
            }
            // Pattern matches for *.csproj, *.sln
            if (lower.endsWith(".csproj") || lower.endsWith(".sln")) {
                manifestFiles.add(prefix + file);
                detectedTech.add("C#");
                detectedTech.add(".NET");
            }
            // docker-compose variants
            if (lower.startsWith("docker-compose") && (lower.endsWith(".yml") || lower.endsWith(".yaml"))) {
                if (!manifestFiles.contains(prefix + file)) {
                    manifestFiles.add(prefix + file);
                    detectedTech.add("Docker Compose");
                }
            }
        }
    }

    private String determinePrimaryLanguage(Map<String, Long> languages, List<String> manifestFiles, Set<String> detectedTech) {
        // Priority 1: GitHub Languages API (highest byte count)
        if (!languages.isEmpty()) {
            return languages.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");
        }

        // Priority 2: Infer from manifest files
        for (Map.Entry<String, String[]> entry : MANIFEST_MAP.entrySet()) {
            String manifestLower = entry.getKey().toLowerCase();
            for (String mf : manifestFiles) {
                if (mf.toLowerCase().endsWith(manifestLower) && entry.getValue()[0] != null) {
                    return entry.getValue()[0];
                }
            }
        }
        // Check .csproj
        if (manifestFiles.stream().anyMatch(f -> f.endsWith(".csproj") || f.endsWith(".sln"))) {
            return "C#";
        }

        // Priority 3: First actual language in detectedTech (not a framework/tool)
        for (String tech : detectedTech) {
            if (!NON_LANGUAGES.contains(tech)) {
                return tech;
            }
        }

        return "Unknown";
    }

    private String determineCategory(Set<String> detectedTech, String primaryLanguage) {
        boolean hasBackendFramework = detectedTech.stream().anyMatch(t ->
                Set.of("Spring Boot", "Django", "FastAPI", "Flask", "Express", "NestJS", "gin", "fiber", "actix", "rocket").contains(t));
        boolean hasFrontendFramework = detectedTech.stream().anyMatch(t ->
                Set.of("React", "Angular", "Vue", "Next.js", "Vite").contains(t));

        if (hasBackendFramework && hasFrontendFramework) return "Full-Stack Web Application";
        if (hasBackendFramework) return "Backend Service";
        if (hasFrontendFramework) return "Frontend Application";
        if (detectedTech.contains("Flutter") || detectedTech.contains("Dart")) return "Mobile Application";
        if (detectedTech.contains("Docker") || detectedTech.contains("Docker Compose")) return "Containerized Application";
        if (!"Unknown".equals(primaryLanguage)) return primaryLanguage + " Project";
        return "Software Project";
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    private String extensionToLanguage(String ext) {
        return switch (ext.toLowerCase()) {
            case ".java" -> "Java";
            case ".kt", ".kts" -> "Kotlin";
            case ".py" -> "Python";
            case ".js", ".mjs" -> "JavaScript";
            case ".ts", ".tsx" -> "TypeScript";
            case ".jsx" -> "JavaScript";
            case ".go" -> "Go";
            case ".rs" -> "Rust";
            case ".php" -> "PHP";
            case ".cs" -> "C#";
            case ".swift" -> "Swift";
            case ".dart" -> "Dart";
            case ".rb" -> "Ruby";
            case ".ex", ".exs" -> "Elixir";
            case ".c", ".h" -> "C";
            case ".cpp", ".cc", ".cxx", ".hpp" -> "C++";
            case ".scala" -> "Scala";
            default -> null;
        };
    }

    private void addIfContains(String text, String keyword, Set<String> techs, String techName) {
        if (text.contains(keyword)) {
            techs.add(techName);
        }
    }
}
