package com.example.gitpilot.memory.entity;

import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contextual_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(nullable = false)
    private String title;

    private String category;

    private String priority;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "estimated_effort")
    private String estimatedEffort;

    @Column(name = "expected_impact")
    private String expectedImpact;

    @Column(name = "target_module")
    private String targetModule;

    @Column(name = "action_taken")
    private Boolean actionTaken = false;
}
