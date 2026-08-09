package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.RecommendationDto;
import com.example.gitpilot.memory.entity.Recommendation;
import com.example.gitpilot.memory.repository.RecommendationRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final RepositoryRepository repositoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final CommitRepository commitRepository;

    public RecommendationService(RepositoryRepository repositoryRepository,
                                 RecommendationRepository recommendationRepository,
                                 CommitRepository commitRepository) {
        this.repositoryRepository = repositoryRepository;
        this.recommendationRepository = recommendationRepository;
        this.commitRepository = commitRepository;
    }

    @Transactional
    public List<RecommendationDto> getRecommendations(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        List<Recommendation> recs = recommendationRepository.findByRepository(repository);
        if (recs.isEmpty()) {
            recs = generateRepositorySpecificRecommendations(repository);
        }

        return recs.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<Recommendation> generateRepositorySpecificRecommendations(Repository repository) {
        List<Recommendation> list = new ArrayList<>();
        String repoName = repository.getName() != null ? repository.getName() : "repository";
        Long commitCount = commitRepository.countByRepository(repository);
        long contributorCount = commitRepository.findContributorsByRepository(repository).size();

        log.info("[Recommendations] Generating repository-specific recommendations for {} (commits={}, contributors={})",
                repoName, commitCount, contributorCount);

        // Recommendation 1: Based on commit volume
        if (commitCount < 10) {
            list.add(createRec(repository,
                    "Increase commit frequency for " + repoName,
                    "ACTIVITY", "MEDIUM",
                    "Repository has only " + commitCount + " commits. Regular, atomic commits improve traceability and code review quality.",
                    "Ongoing", "Better git bisect capability and change tracking", "All Modules"));
        } else {
            list.add(createRec(repository,
                    "Add database index on commit lookups for " + repoName,
                    "DATABASE", "HIGH",
                    "With " + commitCount + " commits, query performance for commit history can be optimized with targeted indexes.",
                    "2 hours", "Faster commit timeline rendering and contributor queries", "Database Layer"));
        }

        // Recommendation 2: Based on contributor count
        if (contributorCount <= 1) {
            list.add(createRec(repository,
                    "Reduce bus-factor risk in " + repoName,
                    "TEAM", "HIGH",
                    "Only " + contributorCount + " contributor detected. Single-maintainer repositories carry high knowledge-loss risk.",
                    "Ongoing", "Knowledge distribution and project resilience", "All Modules"));
        } else {
            list.add(createRec(repository,
                    "Establish code ownership guidelines for " + repoName,
                    "TEAM", "MEDIUM",
                    contributorCount + " contributors active. Define CODEOWNERS file to formalize review responsibilities.",
                    "1 hour", "Faster PR reviews and clear accountability", "Repository Root"));
        }

        // Recommendation 3: Based on repository metadata
        if (repository.getDefaultBranch() != null && repository.getDefaultBranch().equals("master")) {
            list.add(createRec(repository,
                    "Rename default branch from master to main",
                    "BEST_PRACTICE", "LOW",
                    "The default branch is 'master'. Industry convention has shifted to 'main' for inclusive naming.",
                    "30 minutes", "Alignment with GitHub defaults and community standards", "Repository Config"));
        } else {
            list.add(createRec(repository,
                    "Add branch protection rules for " + (repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main"),
                    "SECURITY", "HIGH",
                    "Protect the default branch with required reviews, status checks, and no force-push.",
                    "30 minutes", "Prevents accidental destructive pushes to production", "Repository Config"));
        }

        // Recommendation 4: Testing
        list.add(createRec(repository,
                "Expand automated test coverage for " + repoName,
                "TESTING", "HIGH",
                "Automated tests ensure refactoring safety and catch regressions before deployment.",
                "4-8 hours", "Reduced production defects and confident deployments", "Test Suite"));

        // Recommendation 5: Documentation
        list.add(createRec(repository,
                "Improve onboarding documentation in " + repoName,
                "DOCUMENTATION", "MEDIUM",
                "Clear README, architecture diagrams, and setup guides reduce new-contributor ramp-up time.",
                "2-3 hours", "50% faster contributor onboarding", "Documentation"));

        return recommendationRepository.saveAll(list);
    }

    private Recommendation createRec(Repository repo, String title, String cat, String priority,
                                     String reason, String effort, String impact, String module) {
        Recommendation r = new Recommendation();
        r.setRepository(repo);
        r.setTitle(title);
        r.setCategory(cat);
        r.setPriority(priority);
        r.setReason(reason);
        r.setEstimatedEffort(effort);
        r.setExpectedImpact(impact);
        r.setTargetModule(module);
        r.setActionTaken(false);
        return r;
    }

    private RecommendationDto toDto(Recommendation r) {
        return RecommendationDto.builder()
                .id(r.getId())
                .repositoryId(r.getRepository().getId())
                .title(r.getTitle())
                .category(r.getCategory())
                .priority(r.getPriority())
                .reason(r.getReason())
                .estimatedEffort(r.getEstimatedEffort())
                .expectedImpact(r.getExpectedImpact())
                .targetModule(r.getTargetModule())
                .actionTaken(r.getActionTaken())
                .build();
    }
}
