package com.example.gitpilot.analysis;

import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto;
import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.analysis.service.RepositoryFingerprintService;
import com.example.gitpilot.analysis.service.TechnologyDetectionService;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.repository.entity.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TechnologyDetectionServiceTest {

    @Mock
    private GithubClient githubClient;

    @Mock
    private RepositoryFingerprintService fingerprintService;

    @Test
    void testDetectTechStackSpringAndReact() {
        // No token -> fingerprint unresolved; README path (Priority 4) still applies.
        lenient().when(fingerprintService.fingerprint(any(), any(), any()))
                .thenReturn(RepositoryFingerprintDto.builder().resolved(false).build());
        TechnologyDetectionService service = new TechnologyDetectionService(githubClient, fingerprintService);

        Repository repo = new Repository();
        repo.setName("gitpilot-fullstack");

        String readme = "Full stack app built with Spring Boot 4, React, Docker, Flyway, and PostgreSQL.";
        List<String> commits = List.of(
                "feat: add pom.xml and package.json",
                "feat: add Dockerfile and docker-compose.yml",
                "feat: add flyway V1 migration script for postgresql"
        );

        TechStackDto dto = service.detectTechStack(repo, null, readme, commits);

        assertNotNull(dto);
        // README detection (Priority 4) should find these frameworks/tools
        assertTrue(dto.getDetectedTechnologies().contains("Spring Boot"));
        assertTrue(dto.getDetectedTechnologies().contains("React"));
        assertTrue(dto.getDetectedTechnologies().contains("Docker"));
        assertTrue(dto.getDetectedTechnologies().contains("Flyway"));
        assertTrue(dto.getDetectedTechnologies().contains("PostgreSQL"));
    }
}
