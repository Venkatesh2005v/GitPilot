package com.example.gitpilot.ai.repository;

import com.example.gitpilot.ai.entity.AIReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIReportRepository extends JpaRepository<AIReport, Long> {
    Optional<AIReport> findFirstByRepositoryAndReportTypeOrderByGeneratedTimeDesc(
            com.example.gitpilot.repository.entity.Repository repository, String reportType);
}
