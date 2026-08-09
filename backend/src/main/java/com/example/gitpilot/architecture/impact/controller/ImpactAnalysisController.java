package com.example.gitpilot.architecture.impact.controller;

import com.example.gitpilot.architecture.impact.dto.ImpactReportDto;
import com.example.gitpilot.architecture.impact.service.ChangeImpactService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/architecture/impact")
@RequiredArgsConstructor
public class ImpactAnalysisController {

    private final ChangeImpactService changeImpactService;

    @PostMapping("/{repositoryId}")
    public ResponseEntity<ImpactReportDto> analyzeImpact(
            @PathVariable Long repositoryId,
            @RequestBody(required = false) List<String> modifiedFiles,
            @RequestParam(required = false) String commitHash) {
        ImpactReportDto report = changeImpactService.analyzeImpact(repositoryId, modifiedFiles, commitHash);
        return ResponseEntity.ok(report);
    }
}
