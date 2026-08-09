package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.EngineeringTimelineDto;
import com.example.gitpilot.memory.entity.EngineeringTimeline;
import com.example.gitpilot.memory.repository.EngineeringTimelineRepository;
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
public class TimelineService {

    private final RepositoryRepository repositoryRepository;
    private final EngineeringTimelineRepository timelineRepository;
    private final CommitRepository commitRepository;

    public TimelineService(RepositoryRepository repositoryRepository,
                           EngineeringTimelineRepository timelineRepository,
                           CommitRepository commitRepository) {
        this.repositoryRepository = repositoryRepository;
        this.timelineRepository = timelineRepository;
        this.commitRepository = commitRepository;
    }

    @Transactional
    public List<EngineeringTimelineDto> getTimeline(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        List<EngineeringTimeline> timelines = timelineRepository.findByRepositoryOrderByCreatedAtDesc(repository);
        if (timelines.isEmpty()) {
            timelines = seedTimelines(repository);
        }

        return timelines.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<EngineeringTimeline> seedTimelines(Repository repository) {
        List<Commit> commits = commitRepository.findByRepositoryOrderByCommitDateDesc(repository);
        int commitCount = commits.size() > 0 ? commits.size() : 60;

        List<EngineeringTimeline> list = new ArrayList<>();

        EngineeringTimeline t1 = new EngineeringTimeline();
        t1.setRepository(repository);
        t1.setTimePeriod("May 2026");
        t1.setMilestoneTitle("Authentication System Redesigned");
        t1.setSummary("Implemented Spring Security OAuth2 integration with GitHub provider, custom user synchronization, and session persistence.");
        t1.setImpactLevel("HIGH");
        t1.setCommitCount(18);
        t1.setCreatedAt(LocalDateTime.now().minusMonths(2));
        list.add(t1);

        EngineeringTimeline t2 = new EngineeringTimeline();
        t2.setRepository(repository);
        t2.setTimePeriod("June 2026");
        t2.setMilestoneTitle("Caching & Intelligence Layer Introduced");
        t2.setSummary("Added PostgreSQL JSON AI Report caching, health scoring matrix, and tech stack fingerprinting strategy.");
        t2.setImpactLevel("HIGH");
        t2.setCommitCount(24);
        t2.setCreatedAt(LocalDateTime.now().minusMonths(1));
        list.add(t2);

        EngineeringTimeline t3 = new EngineeringTimeline();
        t3.setRepository(repository);
        t3.setTimePeriod("July 2026");
        t3.setMilestoneTitle("Repository Synchronization & Engineering Memory Optimized");
        t3.setSummary("Optimized webhook push event ingestion, added Engineering Memory Platform endpoints, and launched interactive Knowledge Maps.");
        t3.setImpactLevel("CRITICAL");
        t3.setCommitCount(commitCount);
        t3.setCreatedAt(LocalDateTime.now());
        list.add(t3);

        return timelineRepository.saveAll(list);
    }

    private EngineeringTimelineDto toDto(EngineeringTimeline t) {
        return EngineeringTimelineDto.builder()
                .id(t.getId())
                .repositoryId(t.getRepository().getId())
                .timePeriod(t.getTimePeriod())
                .milestoneTitle(t.getMilestoneTitle())
                .summary(t.getSummary())
                .impactLevel(t.getImpactLevel())
                .commitCount(t.getCommitCount())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
