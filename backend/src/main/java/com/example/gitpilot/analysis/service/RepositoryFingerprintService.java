package com.example.gitpilot.analysis.service;

import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto;
import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto.TechnologyEvidenceDto;
import com.example.gitpilot.github.client.GithubClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a deterministic {@link RepositoryFingerprintDto} by inspecting the CONTENTS of a small,
 * targeted set of manifest/config files (pom.xml, build.gradle, package.json, requirements/pyproject,
 * Docker, CI, application.properties/yml). Dependency/config evidence is treated as STRONG; file
 * structure as MEDIUM; GitHub language stats as supporting. README is intentionally NOT used here.
 *
 * Fully fail-safe: any missing/erroring file is skipped and detection continues. GitHub calls are
 * bounded (root + a limited set of subproject dirs, each listed once) and reuse the existing GithubClient.
 */
@Slf4j
@Service
public class RepositoryFingerprintService {

    private final GithubClient githubClient;

    private static final int MAX_SUBDIRS = 6;
    private static final List<String> KNOWN_SUBPROJECT_ROOTS = List.of(
            "backend", "frontend", "server", "client", "api", "web", "app", "service");
    private static final Set<String> NON_PROJECT_DIRS = Set.of(
            ".git", ".github", ".idea", ".vscode", "node_modules", "target", "dist",
            "build", "docs", "doc", "screenshots", "assets", ".mvn");

    public RepositoryFingerprintService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    public RepositoryFingerprintDto fingerprint(String owner, String repo, String accessToken) {
        if (owner == null || owner.isBlank() || repo == null || repo.isBlank() || accessToken == null) {
            return RepositoryFingerprintDto.builder().resolved(false).build();
        }

        List<String> rootFiles = safeList(owner, repo, "", accessToken);
        if (rootFiles.isEmpty()) {
            return RepositoryFingerprintDto.builder().resolved(false).build();
        }

        // List each candidate directory exactly once.
        Map<String, List<String>> dirListing = new LinkedHashMap<>();
        dirListing.put("", rootFiles);
        for (String dir : candidateSubdirs(rootFiles)) {
            dirListing.put(dir, safeList(owner, repo, dir, accessToken));
        }

        List<TechnologyEvidenceDto> evidence = new ArrayList<>();
        Set<String> languages = new LinkedHashSet<>();
        Set<String> backendLibs = new LinkedHashSet<>();
        Set<String> frontendLibs = new LinkedHashSet<>();
        Set<String> testing = new LinkedHashSet<>();

        RepositoryFingerprintDto.RepositoryFingerprintDtoBuilder fp = RepositoryFingerprintDto.builder();
        String[] db = {null};                 // mutable holders (used across helpers)
        String[] backendFramework = {null};
        String[] backendBuildTool = {null};
        String[] frontendFramework = {null};
        String[] frontendBuildTool = {null};

        // Supporting evidence: GitHub language statistics.
        String primaryLanguage = null;
        try {
            Map<String, Long> langStats = githubClient.getLanguages(owner, repo, accessToken);
            if (langStats != null) {
                langStats.keySet().forEach(languages::add);
                primaryLanguage = langStats.entrySet().stream()
                        .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            }
        } catch (Exception ignored) {}

        for (Map.Entry<String, List<String>> e : dirListing.entrySet()) {
            String dir = e.getKey();
            List<String> files = e.getValue();

            // ---- Java / Maven ----
            if (has(files, "pom.xml")) {
                String path = joinPath(dir, "pom.xml");
                String pom = safeContent(owner, repo, path, accessToken).toLowerCase();
                if (!pom.isBlank()) {
                    languages.add("Java");
                    addEvidence(evidence, "Java", "LANGUAGE", path + " present", "STRONG");
                    backendBuildTool[0] = "Maven";
                    addEvidence(evidence, "Maven", "BACKEND", path + " build tool", "STRONG");
                    if (pom.contains("spring-boot")) { backendFramework[0] = "Spring Boot"; addEvidence(evidence, "Spring Boot", "BACKEND", path + " dependency", "STRONG"); }
                    if (pom.contains("spring-security")) backendLibs.add("Spring Security");
                    if (pom.contains("spring-boot-starter-data-jpa") || pom.contains("hibernate")) backendLibs.add("Hibernate/JPA");
                    if (pom.contains("flyway")) backendLibs.add("Flyway");
                    if (pom.contains("junit")) { testing.add("JUnit"); addEvidence(evidence, "JUnit", "TESTING", path + " dependency", "STRONG"); }
                    if (pom.contains("mockito")) testing.add("Mockito");
                    detectDbFromText(pom, db, evidence, path + " dependency");
                }
            }

            // ---- Gradle ----
            for (String gradle : List.of("build.gradle", "build.gradle.kts")) {
                if (has(files, gradle)) {
                    String path = joinPath(dir, gradle);
                    String c = safeContent(owner, repo, path, accessToken).toLowerCase();
                    if (!c.isBlank()) {
                        languages.add(gradle.endsWith(".kts") ? "Kotlin" : "Java");
                        backendBuildTool[0] = "Gradle";
                        addEvidence(evidence, "Gradle", "BACKEND", path + " build tool", "STRONG");
                        if (c.contains("spring-boot") || c.contains("org.springframework.boot")) { backendFramework[0] = "Spring Boot"; addEvidence(evidence, "Spring Boot", "BACKEND", path + " dependency", "STRONG"); }
                        if (c.contains("junit")) testing.add("JUnit");
                        detectDbFromText(c, db, evidence, path + " dependency");
                    }
                }
            }

            // ---- Node / package.json ----
            if (has(files, "package.json")) {
                String path = joinPath(dir, "package.json");
                String pkg = safeContent(owner, repo, path, accessToken).toLowerCase();
                if (!pkg.isBlank()) {
                    languages.add(has(files, "tsconfig.json") ? "TypeScript" : "JavaScript");
                    if (pkg.contains("\"next\"")) { frontendFramework[0] = "Next.js"; addEvidence(evidence, "Next.js", "FRONTEND", path + " dependency", "STRONG"); }
                    else if (pkg.contains("\"react\"")) { frontendFramework[0] = "React"; addEvidence(evidence, "React", "FRONTEND", path + " dependency", "STRONG"); }
                    else if (pkg.contains("\"vue\"")) { frontendFramework[0] = "Vue"; addEvidence(evidence, "Vue", "FRONTEND", path + " dependency", "STRONG"); }
                    else if (pkg.contains("\"@angular/core\"")) { frontendFramework[0] = "Angular"; addEvidence(evidence, "Angular", "FRONTEND", path + " dependency", "STRONG"); }
                    if (pkg.contains("\"vite\"")) { frontendBuildTool[0] = "Vite"; addEvidence(evidence, "Vite", "FRONTEND", path + " dependency", "STRONG"); }
                    else if (pkg.contains("\"webpack\"")) frontendBuildTool[0] = "webpack";
                    if (pkg.contains("\"express\"")) { backendFramework[0] = backendFramework[0] != null ? backendFramework[0] : "Express"; if ("Express".equals(backendFramework[0])) addEvidence(evidence, "Express", "BACKEND", path + " dependency", "STRONG"); }
                    else if (pkg.contains("\"@nestjs/core\"")) { backendFramework[0] = backendFramework[0] != null ? backendFramework[0] : "NestJS"; if ("NestJS".equals(backendFramework[0])) addEvidence(evidence, "NestJS", "BACKEND", path + " dependency", "STRONG"); }
                    if (pkg.contains("\"redux\"")) frontendLibs.add("Redux");
                    if (pkg.contains("\"tailwindcss\"")) frontendLibs.add("Tailwind CSS");
                    if (pkg.contains("\"jest\"")) { testing.add("Jest"); addEvidence(evidence, "Jest", "TESTING", path + " dependency", "STRONG"); }
                    if (pkg.contains("\"vitest\"")) testing.add("Vitest");
                    if (pkg.contains("\"@playwright/test\"") || pkg.contains("\"playwright\"")) testing.add("Playwright");
                    if (pkg.contains("\"pg\"")) { db[0] = "PostgreSQL"; addEvidence(evidence, "PostgreSQL", "DATABASE", path + " driver", "STRONG"); }
                    else if (pkg.contains("\"mysql\"") || pkg.contains("\"mysql2\"")) db[0] = db[0] != null ? db[0] : "MySQL";
                    else if (pkg.contains("\"mongodb\"") || pkg.contains("\"mongoose\"")) db[0] = db[0] != null ? db[0] : "MongoDB";
                }
            }

            // ---- Python ----
            String pySrc = null, pyContent = "";
            if (has(files, "requirements.txt")) { pySrc = joinPath(dir, "requirements.txt"); pyContent = safeContent(owner, repo, pySrc, accessToken).toLowerCase(); backendBuildTool[0] = backendBuildTool[0] != null ? backendBuildTool[0] : "pip"; }
            else if (has(files, "pyproject.toml")) { pySrc = joinPath(dir, "pyproject.toml"); pyContent = safeContent(owner, repo, pySrc, accessToken).toLowerCase(); backendBuildTool[0] = backendBuildTool[0] != null ? backendBuildTool[0] : "Poetry"; }
            else if (has(files, "setup.py")) { pySrc = joinPath(dir, "setup.py"); pyContent = safeContent(owner, repo, pySrc, accessToken).toLowerCase(); }
            if (pySrc != null && !pyContent.isBlank()) {
                languages.add("Python");
                if (pyContent.contains("django")) { backendFramework[0] = backendFramework[0] != null ? backendFramework[0] : "Django"; if ("Django".equals(backendFramework[0])) addEvidence(evidence, "Django", "BACKEND", pySrc + " dependency", "STRONG"); }
                else if (pyContent.contains("fastapi")) { backendFramework[0] = backendFramework[0] != null ? backendFramework[0] : "FastAPI"; if ("FastAPI".equals(backendFramework[0])) addEvidence(evidence, "FastAPI", "BACKEND", pySrc + " dependency", "STRONG"); }
                else if (pyContent.contains("flask")) { backendFramework[0] = backendFramework[0] != null ? backendFramework[0] : "Flask"; if ("Flask".equals(backendFramework[0])) addEvidence(evidence, "Flask", "BACKEND", pySrc + " dependency", "STRONG"); }
                if (pyContent.contains("pytest")) { testing.add("pytest"); addEvidence(evidence, "pytest", "TESTING", pySrc + " dependency", "STRONG"); }
                if (pyContent.contains("psycopg") || pyContent.contains("postgres")) { db[0] = "PostgreSQL"; addEvidence(evidence, "PostgreSQL", "DATABASE", pySrc + " driver", "STRONG"); }
            }
        }

        // ---- Database from Spring config (strong: actual datasource URL) ----
        detectDbFromSpringConfig(owner, repo, accessToken, dirListing.keySet(), db, evidence);

        // ---- Docker ----
        boolean docker = anyHas(dirListing, "Dockerfile");
        boolean compose = anyHasPrefix(dirListing, "docker-compose") || anyHas(dirListing, "compose.yml") || anyHas(dirListing, "compose.yaml");
        if (docker) addEvidence(evidence, "Docker", "INFRA", "Dockerfile", "STRONG");
        if (compose) addEvidence(evidence, "Docker Compose", "INFRA", "docker-compose file", "STRONG");

        // ---- CI/CD ----
        String ci = detectCI(owner, repo, accessToken, rootFiles);
        if (ci != null) addEvidence(evidence, ci, "INFRA", ci.equals("GitHub Actions") ? ".github/workflows/*.yml" : "CI config", "STRONG");

        if (primaryLanguage == null && !languages.isEmpty()) primaryLanguage = languages.iterator().next();
        languages.forEach(l -> addEvidenceIfAbsent(evidence, l, "LANGUAGE", "detected", "MEDIUM"));

        return fp
                .resolved(true)
                .primaryLanguage(primaryLanguage)
                .languages(new ArrayList<>(languages))
                .backendFramework(backendFramework[0])
                .backendBuildTool(backendBuildTool[0])
                .backendLibraries(new ArrayList<>(backendLibs))
                .frontendFramework(frontendFramework[0])
                .frontendBuildTool(frontendBuildTool[0])
                .frontendLibraries(new ArrayList<>(frontendLibs))
                .database(db[0])
                .testingFrameworks(new ArrayList<>(testing))
                .docker(docker)
                .dockerCompose(compose)
                .ciProvider(ci)
                .evidence(evidence)
                .build();
    }

    private void detectDbFromText(String lower, String[] db, List<TechnologyEvidenceDto> evidence, String source) {
        if (db[0] != null) return;
        if (lower.contains("postgresql") || lower.contains("postgres")) { db[0] = "PostgreSQL"; addEvidence(evidence, "PostgreSQL", "DATABASE", source, "STRONG"); }
        else if (lower.contains("mysql") || lower.contains("mariadb")) { db[0] = "MySQL"; addEvidence(evidence, "MySQL", "DATABASE", source, "STRONG"); }
        else if (lower.contains("mongodb") || lower.contains("mongo-java-driver")) { db[0] = "MongoDB"; addEvidence(evidence, "MongoDB", "DATABASE", source, "STRONG"); }
    }

    private void detectDbFromSpringConfig(String owner, String repo, String token, Set<String> dirs,
                                          String[] db, List<TechnologyEvidenceDto> evidence) {
        if (db[0] != null) return;
        for (String dir : dirs) {
            String base = dir.isEmpty() ? "" : dir + "/";
            for (String cfg : List.of("src/main/resources/application.properties",
                    "src/main/resources/application.yml", "src/main/resources/application.yaml")) {
                String content = safeContent(owner, repo, base + cfg, token).toLowerCase();
                if (content.isBlank()) continue;
                if (content.contains("jdbc:postgresql") || content.contains("org.postgresql")) { db[0] = "PostgreSQL"; addEvidence(evidence, "PostgreSQL", "DATABASE", base + cfg + " datasource", "STRONG"); return; }
                if (content.contains("jdbc:mysql")) { db[0] = "MySQL"; addEvidence(evidence, "MySQL", "DATABASE", base + cfg + " datasource", "STRONG"); return; }
                if (content.contains("mongodb://") || content.contains("spring.data.mongodb")) { db[0] = "MongoDB"; addEvidence(evidence, "MongoDB", "DATABASE", base + cfg + " datasource", "STRONG"); return; }
            }
        }
    }

    private String detectCI(String owner, String repo, String token, List<String> rootFiles) {
        if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase(".gitlab-ci.yml"))) return "GitLab CI";
        if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase("Jenkinsfile"))) return "Jenkins";
        if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase(".circleci"))) return "CircleCI";
        if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase(".travis.yml"))) return "Travis CI";
        if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase(".github"))) {
            List<String> gh = safeList(owner, repo, ".github", token);
            if (gh.stream().anyMatch(f -> f.equalsIgnoreCase("workflows"))) {
                List<String> wf = safeList(owner, repo, ".github/workflows", token);
                if (wf.stream().anyMatch(f -> { String l = f.toLowerCase(); return l.endsWith(".yml") || l.endsWith(".yaml"); })) return "GitHub Actions";
            }
        }
        return null;
    }

    // ---------- helpers ----------

    private List<String> candidateSubdirs(List<String> rootFiles) {
        LinkedHashSet<String> dirs = new LinkedHashSet<>();
        for (String known : KNOWN_SUBPROJECT_ROOTS) {
            if (rootFiles.stream().anyMatch(f -> f.equalsIgnoreCase(known))) dirs.add(matchCase(rootFiles, known));
        }
        for (String entry : rootFiles) {
            if (entry == null || entry.contains(".")) continue;
            if (NON_PROJECT_DIRS.contains(entry.toLowerCase())) continue;
            dirs.add(entry);
        }
        return dirs.stream().limit(MAX_SUBDIRS).toList();
    }

    private boolean has(List<String> files, String name) {
        return files != null && files.stream().anyMatch(f -> f != null && f.equalsIgnoreCase(name));
    }

    private boolean anyHas(Map<String, List<String>> listing, String name) {
        return listing.values().stream().anyMatch(files -> has(files, name));
    }

    private boolean anyHasPrefix(Map<String, List<String>> listing, String prefix) {
        return listing.values().stream().anyMatch(files ->
                files != null && files.stream().anyMatch(f -> f != null && f.toLowerCase().startsWith(prefix)));
    }

    private String joinPath(String dir, String file) { return dir.isEmpty() ? file : dir + "/" + file; }

    /** Fetch file content, swallowing any exception so one bad optional file never fails detection. */
    private String safeContent(String owner, String repo, String path, String token) {
        try {
            String c = githubClient.getFileContent(owner, repo, path, token);
            return c != null ? c : "";
        } catch (Exception e) {
            log.debug("[Fingerprint] getFileContent failed for {}: {}", path, e.getMessage());
            return "";
        }
    }

    private List<String> safeList(String owner, String repo, String path, String token) {
        try {
            List<String> r = path.isEmpty()
                    ? githubClient.getRepoContents(owner, repo, token)
                    : githubClient.getRepoContentsAtPath(owner, repo, path, token);
            return r != null ? r : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String matchCase(List<String> files, String target) {
        return files.stream().filter(f -> f != null && f.equalsIgnoreCase(target)).findFirst().orElse(target);
    }

    private void addEvidence(List<TechnologyEvidenceDto> list, String tech, String cat, String source, String strength) {
        boolean dup = list.stream().anyMatch(e -> e.getTechnology().equalsIgnoreCase(tech) && e.getSource().equalsIgnoreCase(source));
        if (!dup) list.add(TechnologyEvidenceDto.builder().technology(tech).category(cat).source(source).strength(strength).build());
    }

    private void addEvidenceIfAbsent(List<TechnologyEvidenceDto> list, String tech, String cat, String source, String strength) {
        if (list.stream().noneMatch(e -> e.getTechnology().equalsIgnoreCase(tech))) addEvidence(list, tech, cat, source, strength);
    }
}
