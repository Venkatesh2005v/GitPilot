package com.example.gitpilot.memory.entity;

import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "onboarding_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private String title;

    @Column(name = "why_it_matters", columnDefinition = "TEXT", nullable = false)
    private String whyItMatters;

    @Column(name = "reading_time_minutes", nullable = false)
    private Integer readingTimeMinutes;

    private String difficulty;

    @Column(name = "dependencies_json", columnDefinition = "TEXT")
    private String dependenciesJson;

    @Column(name = "key_files_json", columnDefinition = "TEXT")
    private String keyFilesJson;
}
