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
@Table(name = "repository_journeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "milestone_category")
    private String milestoneCategory;

    @Column(name = "icon_name")
    private String iconName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "release_version")
    private String releaseVersion;

    private String contributor;

    @Column(name = "month_val")
    private String monthVal;

    @Column(name = "year_val")
    private Integer yearVal;
}
