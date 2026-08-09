package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.RepositoryStructure;
import com.example.gitpilot.github.client.GithubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryScannerService {

    private final GithubClient githubClient;
    private static final int MAX_DEPTH = 20;
    private static final int MAX_API_CALLS = 60;
    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "build", "dist", "target", ".gradle",
            ".idea", ".vscode", "__pycache__", ".next", "vendor", "coverage",
            "playwright-report", "test-results", ".mvn", ".github", "static"
    );
    private static final Set<String> SOURCE_EXTS = Set.of(
            ".java", ".kt", ".py", ".js", ".jsx", ".ts", ".tsx",
            ".go", ".rs", ".dart", ".php", ".rb", ".cs", ".swift", ".vue"
    );

    public RepositoryStructure scan(String owner, String repo, String accessToken) {
        log.info("[ArchScan] BFS scan starting for {}/{}", owner, repo);
        List<String> allFiles = new ArrayList<>();
        List<String> allFolders = new ArrayList<>();
        Set<String> extensions = new HashSet<>();
        List<String> manifestFiles = new ArrayList<>();
        Map<String, List<String>> filesByFolder = new HashMap<>();
        int apiCalls = 0;

        // BFS - prioritize source directories
        Deque<String> queue = new ArrayDeque<>();
        queue.add("");

        while (!queue.isEmpty() && apiCalls < MAX_API_CALLS) {
            String path = queue.poll();
            int depth = path.isEmpty() ? 0 : path.split("/").length;
            if (depth > MAX_DEPTH) continue;

            apiCalls++;
            List<String> items;
            try {
                items = path.isEmpty()
                        ? githubClient.getRepoContents(owner, repo, accessToken)
                        : githubClient.getRepoContentsAtPath(owner, repo, path, accessToken);
            } catch (Exception e) {
                log.debug("[ArchScan] Failed '{}': {}", path, e.getMessage());
                continue;
            }

            List<String> filesInFolder = new ArrayList<>();
            for (String item : items) {
                String fullPath = path.isEmpty() ? item : path + "/" + item;
                if (item.contains(".")) {
                    allFiles.add(fullPath);
                    filesInFolder.add(item);
                    extensions.add(item.substring(item.lastIndexOf('.')));
                    if (isManifest(item)) manifestFiles.add(fullPath);
                } else {
                    allFolders.add(fullPath);
                    if (!SKIP_DIRS.contains(item.toLowerCase())) {
                        queue.add(fullPath);
                    }
                }
            }
            filesByFolder.put(path.isEmpty() ? "/" : path, filesInFolder);
        }

        log.info("[ArchScan] Complete {}/{}: files={} folders={} exts={} apiCalls={}",
                owner, repo, allFiles.size(), allFolders.size(), extensions.size(), apiCalls);

        return RepositoryStructure.builder()
                .files(allFiles)
                .folders(allFolders)
                .filesByFolder(filesByFolder)
                .extensions(extensions)
                .manifestFiles(manifestFiles)
                .build();
    }

    private boolean isManifest(String name) {
        String lower = name.toLowerCase();
        return Set.of("pom.xml", "build.gradle", "build.gradle.kts", "package.json",
                "cargo.toml", "go.mod", "requirements.txt", "pyproject.toml",
                "pubspec.yaml", "composer.json", "gemfile", "mix.exs",
                "dockerfile", "docker-compose.yml", "docker-compose.yaml",
                "angular.json", "next.config.js", "next.config.mjs",
                "vite.config.js", "vite.config.ts", "tsconfig.json"
        ).contains(lower);
    }
}
