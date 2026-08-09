package com.example.gitpilot.team.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.example.gitpilot.team.dto.BusFactorReportDto;
import com.example.gitpilot.team.dto.BusFactorReportDto.ContributorOwnershipDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamIntelligenceService {

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;

    @Transactional(readOnly = true)
    public BusFactorReportDto computeTeamIntelligence(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId));

        Long actualId = repository.getId();
        List<Commit> commits = commitRepository.findByRepositoryOrderByCommitDateDesc(repository);

        if (commits.isEmpty()) {
            return BusFactorReportDto.builder()
                    .repositoryId(actualId)
                    .totalContributors(0)
                    .busFactorScore(0)
                    .overallRiskLevel("UNKNOWN")
                    .contributorOwnerships(Collections.emptyList())
                    .highRiskModules(Collections.emptyList())
                    .teamRecommendations(List.of("No commit data available. Sync the repository to generate team intelligence."))
                    .build();
        }

        Map<String, Integer> authorCommitMap = new HashMap<>();
        Map<String, String> authorEmailMap = new HashMap<>();

        for (Commit c : commits) {
            String author = c.getAuthorName() != null ? c.getAuthorName() : "Unknown";
            authorCommitMap.put(author, authorCommitMap.getOrDefault(author, 0) + 1);
            if (c.getAuthorEmail() != null) {
                authorEmailMap.put(author, c.getAuthorEmail());
            }
        }

        int totalCommits = authorCommitMap.values().stream().mapToInt(Integer::intValue).sum();
        List<ContributorOwnershipDto> ownerships = new ArrayList<>();

        int topContributorCommits = 0;
        for (Map.Entry<String, Integer> entry : authorCommitMap.entrySet()) {
            double pct = totalCommits > 0 ? Math.round(((double) entry.getValue() / totalCommits) * 1000.0) / 10.0 : 0.0;
            if (entry.getValue() > topContributorCommits) {
                topContributorCommits = entry.getValue();
            }

            ownerships.add(ContributorOwnershipDto.builder()
                    .authorName(entry.getKey())
                    .authorEmail(authorEmailMap.getOrDefault(entry.getKey(), ""))
                    .commitCount(entry.getValue())
                    .ownershipPercentage(pct)
                    .primaryModules(Collections.emptyList())
                    .build());
        }

        ownerships.sort((a, b) -> Integer.compare(b.getCommitCount(), a.getCommitCount()));

        double topOwnershipPct = totalCommits > 0 ? (double) topContributorCommits / totalCommits : 0.0;
        int busFactor = topOwnershipPct > 0.7 ? 1 : topOwnershipPct > 0.4 ? 2 : Math.min(ownerships.size(), 3);
        String riskLevel = busFactor == 1 ? "HIGH" : busFactor == 2 ? "MEDIUM" : "LOW";

        List<String> recommendations = new ArrayList<>();
        if (busFactor == 1) {
            String topDev = ownerships.get(0).getAuthorName();
            recommendations.add("Single contributor (" + topDev + ") owns " + Math.round(topOwnershipPct * 100) + "% of commits. Consider distributing ownership.");
            recommendations.add("Promote pair programming and code reviews to reduce concentration risk.");
        } else if (busFactor == 2) {
            recommendations.add("Knowledge is concentrated among 2 contributors. Encourage broader participation.");
        } else {
            recommendations.add("Healthy distribution of contributions across the team.");
        }
        recommendations.add("Use GitPilot Onboarding Guides to help new contributors ramp up quickly.");

        return BusFactorReportDto.builder()
                .repositoryId(actualId)
                .totalContributors(ownerships.size())
                .busFactorScore(busFactor)
                .overallRiskLevel(riskLevel)
                .contributorOwnerships(ownerships)
                .highRiskModules(Collections.emptyList())
                .teamRecommendations(recommendations)
                .build();
    }
}
