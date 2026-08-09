package com.example.gitpilot.architecture.drift.controller;

import com.example.gitpilot.architecture.drift.dto.DriftViolationDto.DriftReportSummaryDto;
import com.example.gitpilot.architecture.drift.service.ArchitectureDriftService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/architecture/drift")
@RequiredArgsConstructor
public class DriftViolationController {

    private final ArchitectureDriftService architectureDriftService;

    @GetMapping("/{repositoryId}")
    public ResponseEntity<DriftReportSummaryDto> getDriftReport(@PathVariable Long repositoryId) {
        DriftReportSummaryDto report = architectureDriftService.detectDrift(repositoryId);
        return ResponseEntity.ok(report);
    }
}
