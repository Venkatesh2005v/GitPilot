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
@Table(name = "repository_dnas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDNA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "activity_level", nullable = false)
    private Integer activityLevel = 85;

    @Column(name = "repository_size", nullable = false)
    private Integer repositorySize = 75;

    @Column(name = "architecture_quality", nullable = false)
    private Integer architectureQuality = 92;

    @Column(name = "testing_strength", nullable = false)
    private Integer testingStrength = 88;

    @Column(name = "documentation_quality", nullable = false)
    private Integer documentationQuality = 95;

    @Column(name = "deployment_readiness", nullable = false)
    private Integer deploymentReadiness = 90;

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel = 12;

    @Column(name = "maintainability", nullable = false)
    private Integer maintainability = 94;

    @Column(name = "knowledge_score", nullable = false)
    private Integer knowledgeScore = 96;

    @Column(name = "personality_archetype")
    private String personalityArchetype = "Modular High-Velocity Engine";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
