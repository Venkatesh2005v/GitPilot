package com.example.gitpilot.commit;

import com.example.gitpilot.commit.dto.ChangedFileDto;
import com.example.gitpilot.commit.dto.CommitDetailDto;
import com.example.gitpilot.commit.service.CommitDetailService;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.github.dto.GithubCommitDetailResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommitDetailServiceTest {

    @Mock private RepositoryRepository repositoryRepository;
    @Mock private GithubClient githubClient;

    private CommitDetailService service;

    private static final Long REPO_ID = 7L;
    private static final String SHA = "abc123def456";
    private static final String TOKEN = "tok";

    @BeforeEach
    void setup() {
        service = new CommitDetailService(repositoryRepository, githubClient);
        Repository repo = new Repository();
        repo.setId(REPO_ID);
        repo.setName("GitPilot");
        repo.setHtmlUrl("https://github.com/acme/gitpilot");
        lenient().when(repositoryRepository.findById(REPO_ID)).thenReturn(Optional.of(repo));
    }

    private GithubCommitDetailResponse.File file(String name, String status, int add, int del, String patch, String prev) {
        return new GithubCommitDetailResponse.File(name, status, add, del, add + del, patch, prev, null);
    }

    private GithubCommitDetailResponse sample(List<GithubCommitDetailResponse.File> files, int add, int del) {
        var author = new GithubCommitDetailResponse.Author("Alice", "a@x.com", LocalDateTime.now());
        var commit = new GithubCommitDetailResponse.CommitDetail("feat: thing", author);
        var stats = new GithubCommitDetailResponse.Stats(add + del, add, del);
        return new GithubCommitDetailResponse(SHA, "https://github.com/acme/gitpilot/commit/" + SHA, commit, stats, files);
    }

    // 1 + 2 + 3. Resolves repo by id, derives owner/repo server-side, passes correct sha.
    @Test
    void resolvesRepoAndDerivesOwnerRepoAndSha() {
        when(githubClient.getCommitDetail(any(), any(), any(), any()))
                .thenReturn(sample(List.of(file("A.java", "modified", 3, 1, "@@ patch", null)), 3, 1));

        service.getCommitDetail(REPO_ID, SHA, TOKEN);

        ArgumentCaptor<String> owner = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> repo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sha = ArgumentCaptor.forClass(String.class);
        verify(githubClient).getCommitDetail(owner.capture(), repo.capture(), sha.capture(), eq(TOKEN));
        assertEquals("acme", owner.getValue());
        assertEquals("gitpilot", repo.getValue());
        assertEquals(SHA, sha.getValue());
        verify(repositoryRepository).findById(REPO_ID);
    }

    // 4 + 5 + 6. Maps response, stats, and changed files.
    @Test
    void mapsResponseStatsAndFiles() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(
                        file("src/A.java", "modified", 10, 4, "@@ -1 +1 @@", null),
                        file("B.js", "added", 20, 0, "@@ new @@", null)
                ), 30, 4));

        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);

        assertEquals(SHA, dto.getSha());
        assertEquals("feat: thing", dto.getMessage());
        assertEquals("Alice", dto.getAuthorName());
        assertEquals(30, dto.getAdditions());
        assertEquals(4, dto.getDeletions());
        assertEquals(2, dto.getChangedFileCount());
        assertEquals(2, dto.getFiles().size());
        assertEquals("src/A.java", dto.getFiles().get(0).getFilename());
        assertEquals("added", dto.getFiles().get(1).getStatus());
    }

    // 7. Renamed file maps previousFilename.
    @Test
    void mapsRenamedPreviousFilename() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(file("new/Name.java", "renamed", 0, 0, null, "old/Name.java")), 0, 0));

        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        ChangedFileDto f = dto.getFiles().get(0);
        assertEquals("renamed", f.getStatus());
        assertEquals("old/Name.java", f.getPreviousFilename());
    }

    // 8. Null patch handled: patch null, not truncated.
    @Test
    void nullPatchHandled() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(file("image.png", "added", 0, 0, null, null)), 0, 0));

        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        ChangedFileDto f = dto.getFiles().get(0);
        assertNull(f.getPatch());
        assertFalse(f.isPatchTruncated());
    }

    // 9. Large patch truncated and flagged.
    @Test
    void largePatchTruncated() {
        String big = "x".repeat(CommitDetailService.MAX_PATCH_CHARS + 500);
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(file("Big.java", "modified", 999, 1, big, null)), 999, 1));

        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        ChangedFileDto f = dto.getFiles().get(0);
        assertTrue(f.isPatchTruncated());
        assertEquals(CommitDetailService.MAX_PATCH_CHARS, f.getPatch().length());
    }

    // 10. GitHub 404 -> 404 ResponseStatusException.
    @Test
    void github404MapsToNotFound() {
        when(githubClient.getCommitDetail(any(), any(), any(), any()))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", null, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getCommitDetail(REPO_ID, SHA, TOKEN));
        assertEquals(404, ex.getStatusCode().value());
    }

    // 11a. GitHub 403/rate limit -> 429.
    @Test
    void github403MapsToTooManyRequests() {
        when(githubClient.getCommitDetail(any(), any(), any(), any()))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(403), "Forbidden", null, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getCommitDetail(REPO_ID, SHA, TOKEN));
        assertEquals(429, ex.getStatusCode().value());
    }

    // 11b. GitHub 500 -> 502 bad gateway.
    @Test
    void github500MapsToBadGateway() {
        when(githubClient.getCommitDetail(any(), any(), any(), any()))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(500), "Server Error", null, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getCommitDetail(REPO_ID, SHA, TOKEN));
        assertEquals(502, ex.getStatusCode().value());
    }

    // Repository not found -> 404.
    @Test
    void repositoryNotFound() {
        when(repositoryRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getCommitDetail(999L, SHA, TOKEN));
        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(githubClient);
    }

    // Blank SHA -> 400.
    @Test
    void blankShaRejected() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getCommitDetail(REPO_ID, "  ", TOKEN));
        assertEquals(400, ex.getStatusCode().value());
    }

    // Enhancement: complete vs truncated patch flags on the mapped files.
    @Test
    void completeAndNullPatchFlags() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(
                        file("A.java", "modified", 2, 1, "@@ small @@", null),   // complete
                        file("logo.png", "added", 0, 0, null, null)              // null patch
                ), 2, 1));
        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        assertFalse(dto.getFiles().get(0).isPatchTruncated());
        assertNotNull(dto.getFiles().get(0).getPatch());
        assertNull(dto.getFiles().get(1).getPatch());
        assertFalse(dto.getFiles().get(1).isPatchTruncated());
    }

    // Enhancement: deterministic technical-impact classification from file paths.
    @Test
    void technicalImpactClassifiesAreasAndTechnologies() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(
                        file("backend/src/main/java/App.java", "modified", 10, 2, "@@ @@", null),
                        file("frontend/src/pages/Home.tsx", "modified", 5, 1, "@@ @@", null),
                        file("db/migration/V9__x.sql", "added", 3, 0, "@@ @@", null),
                        file(".github/workflows/ci.yml", "added", 8, 0, "@@ @@", null)
                ), 26, 3));
        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        var impact = dto.getTechnicalImpact();
        assertNotNull(impact);
        assertTrue(impact.getAffectedAreas().contains("Backend"));
        assertTrue(impact.getAffectedAreas().contains("Frontend"));
        assertTrue(impact.getAffectedAreas().contains("Database"));
        assertTrue(impact.getAffectedAreas().contains("CI/CD"));
        assertTrue(impact.getTechnologies().contains("Java"));
        assertTrue(impact.getTechnologies().contains("TypeScript"));
        assertTrue(impact.getTechnologies().contains("SQL"));
        assertEquals("Full-stack (backend + frontend layers)", impact.getArchitecturalArea());
        assertTrue(impact.getSummary().contains("+26/-3"));
    }

    // Enhancement: unclassifiable paths -> "Not determined", never invented.
    @Test
    void technicalImpactNotDeterminedForUnknownPaths() {
        when(githubClient.getCommitDetail(any(), any(), any(), any())).thenReturn(
                sample(List.of(file("LICENSE", "modified", 1, 1, "@@ @@", null)), 1, 1));
        CommitDetailDto dto = service.getCommitDetail(REPO_ID, SHA, TOKEN);
        var impact = dto.getTechnicalImpact();
        assertNotNull(impact);
        assertTrue(impact.getAffectedAreas().isEmpty());
        assertEquals("Not determined", impact.getArchitecturalArea());
    }

    // 13. Only repositoryId + sha are inputs; owner/repo cannot be injected (derived from stored htmlUrl).
    @Test
    void ownerRepoCannotBeInjected() {
        when(githubClient.getCommitDetail(any(), any(), any(), any()))
                .thenReturn(sample(List.of(), 0, 0));

        service.getCommitDetail(REPO_ID, SHA, TOKEN);
        // The owner/repo always come from the tracked repository's htmlUrl, regardless of any client value.
        verify(githubClient).getCommitDetail(eq("acme"), eq("gitpilot"), eq(SHA), eq(TOKEN));
    }
}
