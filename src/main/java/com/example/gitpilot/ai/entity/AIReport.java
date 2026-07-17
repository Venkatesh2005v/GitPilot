package com.example.gitpilot.ai.entity;

import com.example.gitpilot.repository.entity.Repository;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reports")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AIReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(nullable = false)
    private String reportType; // e.g. FULL_REPORT, HEALTH, SUMMARY, RECOMMENDATIONS

    @Column(nullable = false)
    private String provider; // e.g. Google Gemini, Groq, OpenRouter

    @Column(nullable = false)
    private String model;

    private String fallbackUsed;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String generatedReport; // JSON content string

    @Column(nullable = false)
    private LocalDateTime generatedTime;
}
