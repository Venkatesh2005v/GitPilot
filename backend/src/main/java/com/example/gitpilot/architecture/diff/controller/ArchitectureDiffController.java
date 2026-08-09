package com.example.gitpilot.architecture.diff.controller;

import com.example.gitpilot.architecture.diff.dto.KnowledgeDiffDto;
import com.example.gitpilot.architecture.diff.service.KnowledgeDiffService;
import com.example.gitpilot.architecture.release.dto.ReleaseIntelligenceDto;
import com.example.gitpilot.architecture.release.service.ReleaseIntelligenceService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/architecture")
@RequiredArgsConstructor
public class ArchitectureDiffController {

    private final KnowledgeDiffService knowledgeDiffService;
    private final ReleaseIntelligenceService releaseIntelligenceService;

    @GetMapping("/diff/{repositoryId}")
    public ResponseEntity<KnowledgeDiffDto> getKnowledgeDiff(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String sourceRef,
            @RequestParam(required = false) String targetRef) {
        KnowledgeDiffDto diff = knowledgeDiffService.compareKnowledgeGraph(repositoryId, sourceRef, targetRef);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/releases/{repositoryId}")
    public ResponseEntity<ReleaseIntelligenceDto> getReleaseIntelligence(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String tag) {
        ReleaseIntelligenceDto release = releaseIntelligenceService.generateReleaseIntelligence(repositoryId, tag);
        return ResponseEntity.ok(release);
    }
}
