package com.example.gitpilot.analysis;

import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto;
import com.example.gitpilot.analysis.service.RepositoryFingerprintService;
import com.example.gitpilot.github.client.GithubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryFingerprintServiceTest {

    @Mock
    private GithubClient githubClient;

    private RepositoryFingerprintService service;

    private static final String OWNER = "acme";
    private static final String REPO = "app";
    private static final String TOKEN = "tok";

    @BeforeEach
    void setup() {
        service = new RepositoryFingerprintService(githubClient);
        // Default: everything empty/missing unless a test overrides. Prevents brittle strict stubs.
        lenient().when(githubClient.getRepoContents(any(), any(), any())).thenReturn(List.of());
        lenient().when(githubClient.getRepoContentsAtPath(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(githubClient.getLanguages(any(), any(), any())).thenReturn(Map.of());
        lenient().when(githubClient.getFileContent(any(), any(), any(), any())).thenReturn("");
    }

    private void root(String... names) {
        when(githubClient.getRepoContents(OWNER, REPO, TOKEN)).thenReturn(List.of(names));
    }

    // 1. Spring Boot + Maven
    @Test
    void springBootMaven() {
        root("pom.xml", "src");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN)).thenReturn(
                "<project><dependency><artifactId>spring-boot-starter-web</artifactId></dependency>" +
                "<dependency><artifactId>junit-jupiter</artifactId></dependency></project>");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertTrue(fp.isResolved());
        assertEquals("Spring Boot", fp.getBackendFramework());
        assertEquals("Maven", fp.getBackendBuildTool());
        assertTrue(fp.getLanguages().contains("Java"));
        assertTrue(fp.getTestingFrameworks().contains("JUnit"));
        assertTrue(hasEvidence(fp, "Spring Boot", "STRONG"));
    }

    // 2. React + Vite
    @Test
    void reactVite() {
        root("package.json", "vite.config.js");
        when(githubClient.getFileContent(OWNER, REPO, "package.json", TOKEN)).thenReturn(
                "{\"dependencies\":{\"react\":\"^18\"},\"devDependencies\":{\"vite\":\"^5\",\"vitest\":\"^1\"}}");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertEquals("React", fp.getFrontendFramework());
        assertEquals("Vite", fp.getFrontendBuildTool());
        assertTrue(fp.getTestingFrameworks().contains("Vitest"));
        assertTrue(hasEvidence(fp, "React", "STRONG"));
    }

    // 3. PostgreSQL from strong evidence (pom dependency)
    @Test
    void postgresFromDependency() {
        root("pom.xml");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN)).thenReturn(
                "<project><dependency><artifactId>postgresql</artifactId></dependency></project>");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertEquals("PostgreSQL", fp.getDatabase());
        assertTrue(hasEvidence(fp, "PostgreSQL", "STRONG"));
    }

    // 3b. PostgreSQL from application.properties datasource
    @Test
    void postgresFromSpringConfig() {
        root("pom.xml", "src");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN)).thenReturn("<project></project>");
        when(githubClient.getFileContent(OWNER, REPO, "src/main/resources/application.properties", TOKEN))
                .thenReturn("spring.datasource.url=jdbc:postgresql://localhost:5432/app");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertEquals("PostgreSQL", fp.getDatabase());
    }

    // 4. Docker detected
    @Test
    void dockerDetected() {
        root("Dockerfile", "docker-compose.yml", "pom.xml");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN)).thenReturn("<project></project>");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertTrue(fp.isDocker());
        assertTrue(fp.isDockerCompose());
    }

    // 5. GitHub Actions detected
    @Test
    void githubActionsDetected() {
        root(".github", "pom.xml");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN)).thenReturn("<project></project>");
        when(githubClient.getRepoContentsAtPath(OWNER, REPO, ".github", TOKEN)).thenReturn(List.of("workflows"));
        when(githubClient.getRepoContentsAtPath(OWNER, REPO, ".github/workflows", TOKEN)).thenReturn(List.of("ci.yml"));

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertEquals("GitHub Actions", fp.getCiProvider());
        assertTrue(hasEvidence(fp, "GitHub Actions", "STRONG"));
    }

    // 6. Sparse repo (no manifests) -> resolved but mostly empty, no crash
    @Test
    void sparseRepo() {
        root("README.md", "LICENSE");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertTrue(fp.isResolved());
        assertNull(fp.getBackendFramework());
        assertNull(fp.getFrontendFramework());
        assertNull(fp.getDatabase());
        assertFalse(fp.isDocker());
    }

    // 7. Missing optional files must not fail entire detection
    @Test
    void missingFilesDoNotThrow() {
        root("pom.xml", "src");
        when(githubClient.getFileContent(OWNER, REPO, "pom.xml", TOKEN))
                .thenThrow(new RuntimeException("boom")); // simulate GitHub error on one file

        RepositoryFingerprintDto fp = assertDoesNotThrow(() -> service.fingerprint(OWNER, REPO, TOKEN));
        assertTrue(fp.isResolved()); // root listing succeeded; per-file error swallowed
    }

    // 8. README-only mention must NOT override stronger evidence (fingerprint ignores README)
    @Test
    void readmeMentionDoesNotCreateFramework() {
        // Repo actually has a package.json WITHOUT react; README (elsewhere) claims "React application".
        root("package.json");
        when(githubClient.getFileContent(OWNER, REPO, "package.json", TOKEN))
                .thenReturn("{\"dependencies\":{\"express\":\"^4\"}}");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertNull(fp.getFrontendFramework(), "React must not be inferred without a dependency");
        assertEquals("Express", fp.getBackendFramework());
    }

    // 9. Monorepo / subdirectory manifests continue to work
    @Test
    void monorepoSubdirectories() {
        root("backend", "frontend", "README.md");
        when(githubClient.getRepoContentsAtPath(OWNER, REPO, "backend", TOKEN)).thenReturn(List.of("pom.xml"));
        when(githubClient.getRepoContentsAtPath(OWNER, REPO, "frontend", TOKEN)).thenReturn(List.of("package.json"));
        when(githubClient.getFileContent(OWNER, REPO, "backend/pom.xml", TOKEN)).thenReturn(
                "<project><dependency><artifactId>spring-boot-starter-web</artifactId></dependency></project>");
        when(githubClient.getFileContent(OWNER, REPO, "frontend/package.json", TOKEN)).thenReturn(
                "{\"dependencies\":{\"react\":\"^18\"}}");

        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, TOKEN);

        assertEquals("Spring Boot", fp.getBackendFramework());
        assertEquals("React", fp.getFrontendFramework());
    }

    // Guard: unresolved when no token/owner
    @Test
    void unresolvedWithoutToken() {
        RepositoryFingerprintDto fp = service.fingerprint(OWNER, REPO, null);
        assertFalse(fp.isResolved());
    }

    private boolean hasEvidence(RepositoryFingerprintDto fp, String tech, String strength) {
        return fp.getEvidence() != null && fp.getEvidence().stream()
                .anyMatch(e -> e.getTechnology().equalsIgnoreCase(tech) && e.getStrength().equalsIgnoreCase(strength));
    }
}
