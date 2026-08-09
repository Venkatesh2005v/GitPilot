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
@Table(name = "engineering_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "decision_title", nullable = false)
    private String decisionTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rationale;

    private String status = "ACCEPTED";

    private String category;

    @Column(name = "date_inferred")
    private LocalDateTime dateInferred = LocalDateTime.now();

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "impact_summary", columnDefinition = "TEXT")
    private String impactSummary;
}
