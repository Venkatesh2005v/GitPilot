package com.example.gitpilot.analysis.service;

import com.example.gitpilot.ai.gateway.AIGatewayService;
import com.example.gitpilot.analysis.dto.HealthScoreDto;
import com.example.gitpilot.analysis.dto.TechStackDto;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.repository.entity.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class HealthScoreService {

    private final AIGatewayService aiGatewayService;

    public HealthScoreService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public HealthScoreDto calculateHealthScore(Repository repository, TechStackDto techStack, String readmeContent, List<Commit> commits) {
        List<String> manifests = techStack.getDetectedManifestFiles() != null ? techStack.getDetectedManifestFiles() : List.of();
        List<String> techs = techStack.getDetectedTechnologies() != null ? techStack.getDetectedTechnologies() : List.of();
        String techLower = String.join(" ", techs).toLowerCase();
        String manifestLower = String.join(" ", manifests).toLowerCase();

        boolean hasReadme = readmeContent != null && readmeContent.trim().length() > 50;
        boolean hasLicense = manifests.stream().anyMatch(f -> f.toLowerCase().contains("license"));
        boolean hasDocker = manifestLower.contains("dockerfile") || techLower.contains("docker");
        boolean hasCI = manifestLower.contains(".github") || manifestLower.contains("jenkinsfile") || manifestLower.contains(".gitlab-ci");
        boolean hasTests = techLower.contains("junit") || techLower.contains("jest") || techLower.contains("pytest")
                || (readmeContent != null && readmeContent.toLowerCase().contains("test"));
        boolean hasConfigQuality = manifests.size() >= 3;
        boolean hasProperStructure = manifests.size() >= 2;

        int commitCount = commits != null ? commits.size() : 0;
        boolean recentActivity = false;
        if (commits != null && !commits.isEmpty()) {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
            recentActivity = commits.stream().anyMatch(c -> c.getCommitDate() != null && c.getCommitDate().isAfter(oneMonthAgo));
        }

        Set<String> uniqueAuthors = new java.util.HashSet<>();
        if (commits != null) {
            commits.forEach(c -> { if (c.getAuthorName() != null) uniqueAuthors.add(c.getAuthorName()); });
        }
        boolean multipleContributors = uniqueAuthors.size() > 1;

        // Deterministic scoring
        int documentationScore = 0;
        if (hasReadme) documentationScore += 20;
        if (hasLicense) documentationScore += 5;
        documentationScore = Math.min(documentationScore, 25);

        int activityScore = 0;
        if (recentActivity) activityScore += 20;
        else if (commitCount > 0) activityScore += 10;

        int collaborationScore = 0;
        if (multipleContributors) collaborationScore += 15;
        else if (commitCount > 0) collaborationScore += 5;

        int structureScore = 0;
        if (hasDocker) structureScore += 10;
        if (hasCI) structureScore += 10;
        if (hasTests) structureScore += 10;
        if (hasConfigQuality) structureScore += 10;
        if (hasProperStructure) structureScore += 10;
        structureScore = Math.min(structureScore, 40);

        int overall = documentationScore + activityScore + collaborationScore + structureScore;
        overall = Math.min(overall, 100);

        String grade = deriveGrade(overall);

        // Build strengths/weaknesses from actual analysis
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (hasReadme) strengths.add("Documentation present (README detected)");
        else weaknesses.add("No README documentation found");

        if (hasDocker) strengths.add("Containerized deployment (Docker)");
        if (hasCI) strengths.add("CI/CD pipeline configured");
        if (hasTests) strengths.add("Automated testing detected");
        else weaknesses.add("No automated tests detected");

        if (recentActivity) strengths.add("Active development (" + commitCount + " commits)");
        else if (commitCount == 0) weaknesses.add("No commit history available");
        else weaknesses.add("No recent commits in the last 30 days");

        if (multipleContributors) strengths.add(uniqueAuthors.size() + " contributors collaborating");
        else weaknesses.add("Single contributor — consider expanding the team");

        if (!hasLicense) weaknesses.add("No license file detected");

        // Normalized sub-scores for display (out of 100)
        int docNorm = (int) Math.round((documentationScore / 25.0) * 100);
        int actNorm = (int) Math.round((activityScore / 20.0) * 100);
        int colNorm = (int) Math.round((collaborationScore / 15.0) * 100);
        int strNorm = (int) Math.round((structureScore / 40.0) * 100);

        // Generate AI explanation
        String aiExplanation = generateExplanation(repository, overall, grade, strengths, weaknesses, techStack);

        return HealthScoreDto.builder()
                .overallHealthScore(overall)
                .grade(grade)
                .documentationScore(docNorm)
                .activityScore(actNorm)
                .collaborationScore(colNorm)
                .structureScore(strNorm)
                .documentationRating(docNorm + "/100")
                .testingRating(hasTests ? "Detected" : "Not Detected")
                .architectureRating(strNorm + "/100")
                .maintainabilityRating(actNorm + "/100")
                .keyStrengths(strengths)
                .keyWeaknesses(weaknesses)
                .aiExplanation(aiExplanation)
                .build();
    }

    private String deriveGrade(int score) {
        if (score >= 95) return "Excellent";
        if (score >= 80) return "Good";
        if (score >= 65) return "Fair";
        return "Needs Attention";
    }

    private String generateExplanation(Repository repository, int score, String grade,
                                        List<String> strengths, List<String> weaknesses, TechStackDto techStack) {
        String prompt = String.format(
                "Repository '%s' has a health score of %d/100 (Grade: %s).\n" +
                "Strengths: %s\nWeaknesses: %s\nTech stack: %s\n\n" +
                "In 2-3 sentences, explain why this repository received this health grade, what its strongest aspects are, and what would improve the score. Return ONLY plain text, no JSON.",
                repository.getName(), score, grade, strengths, weaknesses, techStack.getDetectedTechnologies()
        );

        try {
            AIGatewayService.AIResult result = aiGatewayService.generateInsightWithFailover(prompt);
            return result.text != null ? result.text.trim() : null;
        } catch (Exception e) {
            log.debug("[AI] Could not generate health explanation for {}: {}", repository.getName(), e.getMessage());
            return "Score based on " + strengths.size() + " strengths and " + weaknesses.size() + " areas for improvement. " +
                    (weaknesses.isEmpty() ? "Repository is well-maintained." : "Key area: " + weaknesses.get(0) + ".");
        }
    }
}
