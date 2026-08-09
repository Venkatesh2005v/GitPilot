package com.example.gitpilot.architecture.techdebt.controller;

import com.example.gitpilot.architecture.techdebt.dto.CodeMetricsDto.RepositoryTechDebtOverviewDto;
import com.example.gitpilot.architecture.techdebt.service.TechnicalDebtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/architecture/tech-debt")
@RequiredArgsConstructor
public class TechnicalDebtController {

    private final TechnicalDebtService technicalDebtService;

    @GetMapping("/{repositoryId}")
    public ResponseEntity<RepositoryTechDebtOverviewDto> getTechDebtMetrics(@PathVariable Long repositoryId) {
        RepositoryTechDebtOverviewDto overview = technicalDebtService.computeTechnicalDebtMetrics(repositoryId);
        return ResponseEntity.ok(overview);
    }
}
