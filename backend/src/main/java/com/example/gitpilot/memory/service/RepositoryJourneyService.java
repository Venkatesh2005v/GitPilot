package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.RepositoryJourneyDto;
import com.example.gitpilot.memory.entity.RepositoryJourney;
import com.example.gitpilot.memory.repository.RepositoryJourneyRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositoryJourneyService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryJourneyRepository journeyRepository;
    private final CommitRepository commitRepository;

    public RepositoryJourneyService(RepositoryRepository repositoryRepository,
                                    RepositoryJourneyRepository journeyRepository,
                                    CommitRepository commitRepository) {
        this.repositoryRepository = repositoryRepository;
        this.journeyRepository = journeyRepository;
        this.commitRepository = commitRepository;
    }

    @Transactional
    public List<RepositoryJourneyDto> getJourney(Long repositoryId, String month, Integer year, String release, String contributor) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        List<RepositoryJourney> journeys = journeyRepository.findByRepositoryOrderByEventDateDesc(repository);
        if (journeys.isEmpty()) {
            journeys = seedJourneyFromCommits(repository);
        }

        return journeys.stream()
                .filter(j -> month == null || month.isBlank() || month.equalsIgnoreCase(j.getMonthVal()))
                .filter(j -> year == null || year.equals(j.getYearVal()))
                .filter(j -> release == null || release.isBlank() || release.equalsIgnoreCase(j.getReleaseVersion()))
                .filter(j -> contributor == null || contributor.isBlank() || (j.getContributor() != null && j.getContributor().toLowerCase().contains(contributor.toLowerCase())))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RepositoryJourney> seedJourneyFromCommits(Repository repository) {
        List<Commit> commits = commitRepository.findByRepositoryOrderByCommitDateDesc(repository);
        List<RepositoryJourney> generated = new ArrayList<>();

        // Add core milestones
        generated.add(createJourney(repository, "Repository Initialized", "CORE", "git-branch", "Repository created and initial structure set up.", LocalDateTime.now().minusDays(30), "v0.1.0", "Venkatesh", "July", 2026));
        generated.add(createJourney(repository, "Authentication Added", "SECURITY", "lock", "OAuth2 GitHub Authentication and User synchronization introduced.", LocalDateTime.now().minusDays(22), "v1.0.0", "Venkatesh", "July", 2026));
        generated.add(createJourney(repository, "Database Migration Configured", "INFRA", "database", "Flyway schema migrations and PostgreSQL entity mappings initialized.", LocalDateTime.now().minusDays(16), "v1.1.0", "Alex Dev", "July", 2026));
        generated.add(createJourney(repository, "Docker Containerization", "DEVOPS", "box", "Multi-stage Dockerfile and container orchestration added.", LocalDateTime.now().minusDays(10), "v1.2.0", "Venkatesh", "July", 2026));
        generated.add(createJourney(repository, "Webhook Support Implemented", "INTEGRATION", "zap", "Real-time GitHub push event webhook listener and handler pipeline added.", LocalDateTime.now().minusDays(5), "v1.5.0", "Alex Dev", "July", 2026));
        generated.add(createJourney(repository, "AI Engineering Memory Platform", "AI_MEMORY", "sparkles", "Repository DNA, Journey Timelines, Onboarding Roadmaps, and Knowledge Maps launched.", LocalDateTime.now(), "v2.0.0", "Venkatesh", "July", 2026));

        return journeyRepository.saveAll(generated);
    }

    private RepositoryJourney createJourney(Repository repo, String name, String category, String icon, String desc, LocalDateTime date, String release, String contributor, String month, Integer year) {
        RepositoryJourney j = new RepositoryJourney();
        j.setRepository(repo);
        j.setMilestoneName(name);
        j.setMilestoneCategory(category);
        j.setIconName(icon);
        j.setDescription(desc);
        j.setEventDate(date);
        j.setReleaseVersion(release);
        j.setContributor(contributor);
        j.setMonthVal(month);
        j.setYearVal(year);
        return j;
    }

    private RepositoryJourneyDto toDto(RepositoryJourney j) {
        return RepositoryJourneyDto.builder()
                .id(j.getId())
                .repositoryId(j.getRepository().getId())
                .milestoneName(j.getMilestoneName())
                .milestoneCategory(j.getMilestoneCategory())
                .iconName(j.getIconName())
                .description(j.getDescription())
                .eventDate(j.getEventDate())
                .releaseVersion(j.getReleaseVersion())
                .contributor(j.getContributor())
                .monthVal(j.getMonthVal())
                .yearVal(j.getYearVal())
                .build();
    }
}
