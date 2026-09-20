package com.example.gitpilot.commit.service;

import com.example.gitpilot.commit.dto.ChangedFileDto;
import com.example.gitpilot.commit.dto.CommitDetailDto;
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

        return CommitDetailDto.builder()
                .sha(gh.getSha() != null ? gh.getSha() : requestedSha)
                .message(commit != null ? commit.getMessage() : null)
                .authorName(author != null ? author.getName() : null)
                .authorEmail(author != null ? author.getEmail() : null)
                .date(author != null ? author.getDate() : null)
                .additions(stats != null && stats.getAdditions() != null ? stats.getAdditions() : 0)
                .deletions(stats != null && stats.getDeletions() != null ? stats.getDeletions() : 0)
                .changedFileCount(files.size())
                .htmlUrl(gh.getHtmlUrl())
                .files(files)
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
