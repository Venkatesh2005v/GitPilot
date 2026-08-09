package com.example.gitpilot.memory.entity;

import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "engineering_timelines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "time_period", nullable = false)
    private String timePeriod;

    @Column(name = "milestone_title", nullable = false)
    private String milestoneTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "impact_level")
    private String impactLevel;

    @Column(name = "commit_count")
    private Integer commitCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
