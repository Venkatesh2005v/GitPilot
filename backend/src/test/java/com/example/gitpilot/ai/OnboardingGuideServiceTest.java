package com.example.gitpilot.ai;

import com.example.gitpilot.ai.dto.OnboardingReportDto;
import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.ai.service.OnboardingGuideService;
import com.example.gitpilot.analysis.dto.RepositoryFingerprintDto;
import com.example.gitpilot.analysis.service.RepositoryEvidenceService;
import com.example.gitpilot.analysis.service.RepositoryEvidenceService.RepositoryEvidence;
import com.example.gitpilot.analysis.service.RepositoryFingerprintService;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingGuideServiceTest {

    @Mock private RepositoryRepository repositoryRepository;
    @Mock private CommitRepository commitRepository;
    @Mock private GithubClient githubClient;
    @Mock private AIGatewayService aiGatewayService;
    @Mock private RepositoryFingerprintService fingerprintService;
    @Mock private RepositoryEvidenceService repositoryEvidenceService;

    private OnboardingGuideService service;

    private static final Long REPO_ID = 42L;

    @BeforeEach
    void setup() {
        service = new OnboardingGuideService(repositoryRepository, commitRepository, githubClient,
                aiGatewayService, fingerprintService, repositoryEvidenceService);

        Repository repo = new Repository();
        repo.setId(REPO_ID);
        repo.setName("GitPilot");
        repo.setHtmlUrl("https://github.com/acme/GitPilot");

        lenient().when(repositoryRepository.findById(REPO_ID)).thenReturn(Optional.of(repo));
        lenient().when(commitRepository.findFirstByRepositoryOrderByCommitDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(commitRepository.findByRepositoryOrderByCommitDateDesc(any(Repository.class))).thenReturn(List.of());
        lenient().when(commitRepository.findContributorsByRepository(any())).thenReturn(List.of());
        lenient().when(githubClient.getReadme(any(), any(), any())).thenReturn("GitPilot manages GitHub repositories.");
        lenient().when(githubClient.getLanguages(any(), any(), any())).thenReturn(Map.of());
        lenient().when(githubClient.getRepoContents(any(), any(), any())).thenReturn(List.of("backend", "frontend", "pom.xml"));
    }

    private RepositoryFingerprintDto resolvedSpringReactFingerprint() {
        return RepositoryFingerprintDto.builder()
                .resolved(true)
                .primaryLanguage("Java")
                .language("Java").language("JavaScript")
                .backendFramework("Spring Boot")
                .backendBuildTool("Maven")
                .frontendFramework("React")
                .frontendBuildTool("Vite")
                .database("PostgreSQL")
                .testingFramework("JUnit")
                .docker(true)
                .build();
    }

    // 1 + 2. Fingerprint is passed into generation AND deterministic tech is preferred over AI guess.
    @Test
    void fingerprintPassedAndDeterministicTechPreferred() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(resolvedSpringReactFingerprint());
        when(repositoryEvidenceService.detect(any(), any(), any(), any()))
                .thenReturn(RepositoryEvidence.builder().resolved(true).hasTests(true).hasCI(true).hasDocker(true).build());
        // AI returns a WRONG/guessed stack — must be overridden by the deterministic fingerprint.
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"p\",\"technologyStack\":[\"COBOL\",\"jQuery\"]}", "Gemini", "m", "None"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");

        // Fingerprint evidence reached the prompt.
        org.mockito.Mockito.verify(aiGatewayService).generateInsightWithFailover(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Spring Boot"));
        assertTrue(prompt.contains("DETERMINISTIC TECHNOLOGY FINGERPRINT"));

        // Deterministic stack preferred over the AI's guess.
        assertTrue(report.getTechnologyStack().contains("Spring Boot"));
        assertTrue(report.getTechnologyStack().contains("React"));
        assertFalse(report.getTechnologyStack().contains("COBOL"));
    }

    // 3 + 4 + 5 + 6. Prompt instructs problem/requirements + strict implemented-vs-mentioned rules.
    @Test
    void promptContainsFoundationAndImplementedVsMentionedRules() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(resolvedSpringReactFingerprint());
        when(repositoryEvidenceService.detect(any(), any(), any(), any())).thenReturn(RepositoryEvidence.unresolved());
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"p\"}", "Gemini", "m", "None"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");
        org.mockito.Mockito.verify(aiGatewayService).generateInsightWithFailover(promptCaptor.capture());
        String prompt = promptCaptor.getValue().toLowerCase();

        assertTrue(prompt.contains("problemsolved"));
        assertTrue(prompt.contains("primaryrequirements"));
        assertTrue(prompt.contains("implementedfeatures"));
        assertTrue(prompt.contains("mentioned")); // implemented-vs-mentioned rule present
        assertTrue(prompt.contains("insufficient repository evidence")); // conservative problem rule
    }

    // AI-provided foundation fields are preserved on the report (parsed from JSON).
    @Test
    void aiFoundationFieldsParsed() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(RepositoryFingerprintDto.builder().resolved(false).build());
        when(repositoryEvidenceService.detect(any(), any(), any(), any())).thenReturn(RepositoryEvidence.unresolved());
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"p\",\"problemSolved\":\"Solves X\"," +
                "\"primaryRequirements\":[\"Auth\",\"Sync\"]," +
                "\"implementedFeatures\":[{\"name\":\"OAuth\",\"description\":\"login\",\"evidence\":\"SecurityConfig present\"}]}",
                "Gemini", "m", "None"));

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");

        assertEquals("Solves X", report.getProblemSolved());
        assertEquals(List.of("Auth", "Sync"), report.getPrimaryRequirements());
        assertEquals(1, report.getImplementedFeatures().size());
        assertEquals("SecurityConfig present", report.getImplementedFeatures().get(0).getEvidence());
    }

    // 7 + 8 + 9. Evidence-based first contributions: tests/CI present => not recommended.
    @Test
    void firstContributionsRespectEvidenceWhenAiOmits() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(resolvedSpringReactFingerprint());
        when(repositoryEvidenceService.detect(any(), any(), any(), any()))
                .thenReturn(RepositoryEvidence.builder().resolved(true).hasTests(true).hasCI(true).hasDocker(true).build());
        // AI omits suggestedFirstContributions -> service fills from evidence.
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"p\"}", "Gemini", "m", "None"));

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");

        List<String> contributions = report.getSuggestedFirstContributions();
        assertNotNull(contributions);
        assertFalse(contributions.isEmpty());
        String joined = String.join(" | ", contributions).toLowerCase();
        // Core contract: never recommend adding tests/CI/docker when evidence confirms they exist.
        assertFalse(joined.contains("add automated tests"), "must not recommend tests when tests exist");
        assertFalse(joined.contains("set up a ci pipeline"), "must not recommend CI when CI exists");
        assertFalse(joined.contains("add containerization") || joined.contains("add docker"), "must not recommend Docker when Docker exists");
    }

    // Evidence confirms tests missing => tests ARE recommended.
    @Test
    void firstContributionsRecommendTestsWhenMissing() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(RepositoryFingerprintDto.builder().resolved(false).build());
        when(repositoryEvidenceService.detect(any(), any(), any(), any()))
                .thenReturn(RepositoryEvidence.builder().resolved(true).hasTests(false).hasCI(false).hasDocker(false).build());
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"p\"}", "Gemini", "m", "None"));

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");
        String joined = String.join(" | ", report.getSuggestedFirstContributions()).toLowerCase();
        assertTrue(joined.contains("add automated tests"));
        assertTrue(joined.contains("set up a ci pipeline"));
    }

    // 10 + 11. Sparse repo + AI unavailable => deterministic fallback, conservative, no crash.
    @Test
    void sparseRepoAiUnavailableFallback() {
        when(githubClient.getReadme(any(), any(), any())).thenReturn(""); // no README
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(RepositoryFingerprintDto.builder().resolved(false).build());
        when(repositoryEvidenceService.detect(any(), any(), any(), any())).thenReturn(RepositoryEvidence.unresolved());
        when(aiGatewayService.generateInsightWithFailover(any())).thenThrow(new RuntimeException("AI down"));

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");

        assertNotNull(report);
        assertEquals(REPO_ID, report.getRepositoryId());
        // Conservative: no invented problem, no invented implemented features.
        assertTrue(report.getImplementedFeatures().isEmpty());
        assertTrue(report.getPrimaryRequirements().isEmpty());
        assertEquals("Insufficient repository evidence to determine the problem solved.", report.getProblemSolved());
        assertFalse(report.getRecommendedLearningPath().isEmpty()); // deterministic learning path still present
    }

    // 12. Existing report fields remain populated/compatible.
    @Test
    void existingFieldsRemainCompatible() {
        when(fingerprintService.fingerprint(any(), any(), any())).thenReturn(resolvedSpringReactFingerprint());
        when(repositoryEvidenceService.detect(any(), any(), any(), any())).thenReturn(RepositoryEvidence.unresolved());
        when(aiGatewayService.generateInsightWithFailover(any())).thenReturn(new AIGatewayService.AIResult(
                "{\"projectPurpose\":\"Purpose here\",\"highLevelDescription\":\"desc\"," +
                "\"recommendedLearningPath\":[\"Step 1\"],\"mainModules\":[{\"moduleName\":\"m\",\"responsibility\":\"r\",\"keyFiles\":[]}]}",
                "Gemini", "m", "None"));

        OnboardingReportDto report = service.getOrGenerateOnboardingGuide(REPO_ID, true, "tok");

        assertEquals("Purpose here", report.getProjectPurpose());
        assertEquals("desc", report.getHighLevelDescription());
        assertEquals(REPO_ID, report.getRepositoryId());
        assertEquals("GitPilot", report.getRepositoryName());
        assertEquals(List.of("Step 1"), report.getRecommendedLearningPath());
        assertEquals(1, report.getMainModules().size());
    }
}
