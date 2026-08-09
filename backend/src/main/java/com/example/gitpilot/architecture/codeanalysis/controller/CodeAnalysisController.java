package com.example.gitpilot.architecture.codeanalysis.controller;

import com.example.gitpilot.architecture.codeanalysis.service.CodeDependencyService;
import com.example.gitpilot.memory.entity.KnowledgeNode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/architecture/code-graph")
@RequiredArgsConstructor
public class CodeAnalysisController {

    private final CodeDependencyService codeDependencyService;

    @PostMapping("/{repositoryId}/analyze")
    public ResponseEntity<List<KnowledgeNode>> analyzeDependencies(
            @PathVariable Long repositoryId,
            @RequestBody(required = false) Map<String, String> fileSourceMap) {
        List<KnowledgeNode> nodes = codeDependencyService.analyzeAndPersistDependencies(repositoryId, fileSourceMap);
        return ResponseEntity.ok(nodes);
    }
}
