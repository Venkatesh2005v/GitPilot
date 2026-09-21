package com.example.gitpilot.commit.service;

import com.example.gitpilot.commit.dto.ChangedFileDto;
import com.example.gitpilot.commit.dto.CommitDetailDto;
import com.example.gitpilot.commit.dto.TechnicalImpactDto;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubCommitDetailResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Serves on-demand commit detail (changed files + patches) by resolving a TRACKED repository
 * from repositoryId server-side and calling GitHub with the authenticated user's token.
 *
 * Security: the client supplies only repositoryId + sha. owner/repo are derived from the stored
 * repository's htmlUrl — never accepted from the request — so this is not a generic GitHub proxy.
 * Nothing here is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitDetailService {

    /** Per-file patch character cap. Larger patches are truncated and flagged. */
    public static final int MAX_PATCH_CHARS = 20_000;

    private final RepositoryRepository repositoryRepository;
    private final GithubClient githubClient;

    public CommitDetailDto getCommitDetail(Long repositoryId, String sha, String accessToken) {
        if (sha == null || sha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commit SHA is required");
        }

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        String[] ownerRepo = resolveOwnerRepo(repository);
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];
        if (owner.isEmpty() || repo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository does not have a resolvable GitHub URL");
        }

        GithubCommitDetailResponse gh;
        try {
            gh = githubClient.getCommitDetail(owner, repo, sha, accessToken);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Commit not found: " + sha);
            }
            if (status == 403 || status == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "GitHub rate limit or access denied. Please try again later.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub error while fetching commit detail (HTTP " + status + ")");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch commit detail from GitHub");
        }

        if (gh == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Commit not found: " + sha);
        }

        return mapToDto(gh, sha);
    }

    private CommitDetailDto mapToDto(GithubCommitDetailResponse gh, String requestedSha) {
        GithubCommitDetailResponse.CommitDetail commit = gh.getCommit();
        GithubCommitDetailResponse.Author author = commit != null ? commit.getAuthor() : null;
        GithubCommitDetailResponse.Stats stats = gh.getStats();

        List<ChangedFileDto> files = new ArrayList<>();
        if (gh.getFiles() != null) {
            for (GithubCommitDetailResponse.File f : gh.getFiles()) {
                files.add(mapFile(f));
            }
        }

        int additions = stats != null && stats.getAdditions() != null ? stats.getAdditions() : 0;
        int deletions = stats != null && stats.getDeletions() != null ? stats.getDeletions() : 0;

        return CommitDetailDto.builder()
                .sha(gh.getSha() != null ? gh.getSha() : requestedSha)
                .message(commit != null ? commit.getMessage() : null)
                .authorName(author != null ? author.getName() : null)
                .authorEmail(author != null ? author.getEmail() : null)
                .date(author != null ? author.getDate() : null)
                .additions(additions)
                .deletions(deletions)
                .changedFileCount(files.size())
                .htmlUrl(gh.getHtmlUrl())
                .files(files)
                .technicalImpact(computeTechnicalImpact(files, additions, deletions))
                .build();
    }

    private ChangedFileDto mapFile(GithubCommitDetailResponse.File f) {
        String patch = f.getPatch();
        boolean truncated = false;
        if (patch != null && patch.length() > MAX_PATCH_CHARS) {
            patch = patch.substring(0, MAX_PATCH_CHARS);
            truncated = true;
        }
        return ChangedFileDto.builder()
                .filename(f.getFilename())
                .status(f.getStatus())
                .additions(f.getAdditions() != null ? f.getAdditions() : 0)
                .deletions(f.getDeletions() != null ? f.getDeletions() : 0)
                .changes(f.getChanges() != null ? f.getChanges() : 0)
                .previousFilename(f.getPreviousFilename())
                .patch(patch)                 // null stays null (binary/omitted)
                .patchTruncated(truncated)
                .blobUrl(f.getBlobUrl())
                .build();
    }

    /**
     * Deterministic technical impact derived ONLY from changed-file paths/extensions. No AI,
     * no speculation. When nothing can be classified, returns an explicit "Not determined" state.
     */
    TechnicalImpactDto computeTechnicalImpact(List<ChangedFileDto> files, int additions, int deletions) {
        java.util.LinkedHashSet<String> areas = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> techs = new java.util.LinkedHashSet<>();

        for (ChangedFileDto f : files) {
            String path = f.getFilename() != null ? f.getFilename().toLowerCase() : "";
            if (path.isEmpty()) continue;

            // Area classification (path/name based).
            if (path.startsWith(".github/workflows/") || path.equals(".gitlab-ci.yml") || path.equalsIgnoreCase("jenkinsfile")) areas.add("CI/CD");
            else if (path.contains("dockerfile") || path.startsWith("docker-compose") || path.contains("/k8s/") || path.endsWith(".tf")) areas.add("Infrastructure");
            else if (path.contains("/test/") || path.contains("/tests/") || path.contains("__tests__") || path.matches(".*\\.(test|spec)\\.[jt]sx?$") || path.matches(".*(test_|_test)\\.py$")) areas.add("Testing");
            else if (path.matches(".*\\.(md|mdx|rst|txt|adoc)$")) areas.add("Documentation");
            else if (path.matches(".*\\.(sql)$") || path.contains("/migration/") || path.contains("/db/")) areas.add("Database");
            else if (path.matches(".*\\.(properties|ya?ml|toml|ini|env|cfg|conf)$") || path.contains(".env")) areas.add("Configuration");
            else if (path.matches(".*\\.(jsx?|tsx?|vue|css|scss|html)$") || path.contains("/frontend/") || path.contains("/src/components/") || path.contains("/src/pages/")) areas.add("Frontend");
            else if (path.matches(".*\\.(java|kt|kts|go|rb|cs|php)$") || path.contains("/backend/") || path.contains("/src/main/")) areas.add("Backend");

            // Technology classification (extension based; only concrete file types).
            if (path.endsWith(".java")) techs.add("Java");
            else if (path.endsWith(".kt") || path.endsWith(".kts")) techs.add("Kotlin");
            else if (path.endsWith(".ts") || path.endsWith(".tsx")) techs.add("TypeScript");
            else if (path.endsWith(".js") || path.endsWith(".jsx")) techs.add("JavaScript");
            else if (path.endsWith(".py")) techs.add("Python");
            else if (path.endsWith(".go")) techs.add("Go");
            else if (path.endsWith(".sql")) techs.add("SQL");
            else if (path.endsWith(".css") || path.endsWith(".scss")) techs.add("CSS");
        }

        String architecturalArea;
        if (areas.contains("Backend") && areas.contains("Frontend")) architecturalArea = "Full-stack (backend + frontend layers)";
        else if (areas.contains("Backend")) architecturalArea = "Backend service layer";
        else if (areas.contains("Frontend")) architecturalArea = "Frontend/UI layer";
        else if (areas.contains("Database")) architecturalArea = "Data/persistence layer";
        else if (areas.contains("Infrastructure") || areas.contains("CI/CD")) architecturalArea = "Build/deployment layer";
        else if (!areas.isEmpty()) architecturalArea = String.join(", ", areas);
        else architecturalArea = "Not determined";

        String summary;
        if (files.isEmpty()) {
            summary = "No changed files reported for this commit.";
        } else if (areas.isEmpty()) {
            summary = "Changed " + files.size() + " file(s) (+" + additions + "/-" + deletions + "). Affected area not determined from file paths.";
        } else {
            summary = "Changed " + files.size() + " file(s) across " + String.join(", ", areas) + " (+" + additions + "/-" + deletions + ").";
        }

        return TechnicalImpactDto.builder()
                .affectedAreas(new ArrayList<>(areas))
                .technologies(new ArrayList<>(techs))
                .architecturalArea(architecturalArea)
                .summary(summary)
                .build();
    }

    /** Derive owner/repo from the repository's stored htmlUrl (never from client input). */
    private String[] resolveOwnerRepo(Repository repository) {
        String owner = "";
        String repo = "";
        String htmlUrl = repository.getHtmlUrl();
        if (htmlUrl != null && htmlUrl.contains("github.com/")) {
            String path = htmlUrl.substring(htmlUrl.indexOf("github.com/") + 11);
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                owner = parts[0];
                repo = parts[1];
            }
        }
        return new String[]{owner, repo};
    }
}
