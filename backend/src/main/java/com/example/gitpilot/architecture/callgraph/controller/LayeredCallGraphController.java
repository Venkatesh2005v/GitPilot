package com.example.gitpilot.architecture.callgraph.controller;

import com.example.gitpilot.architecture.analysis.dto.GraphResponse;
import com.example.gitpilot.architecture.analysis.service.RepositoryArchitectureService;
import com.example.gitpilot.security.GitHubTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/architecture/repositories")
@RequiredArgsConstructor
public class LayeredCallGraphController {

    private final RepositoryArchitectureService architectureService;
    private final GitHubTokenResolver tokenResolver;

    @GetMapping("/{id}/callgraph")
    public ResponseEntity<Map<String, Object>> getCallGraph(@PathVariable Long id,
                                                             Authentication authentication,
                                                             HttpServletRequest request) {
        try {
            String accessToken = tokenResolver.resolve(authentication, request);
            GraphResponse graph = architectureService.analyzeRepository(id, accessToken);

            if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
                return ResponseEntity.ok(Map.of("nodes", List.of(), "edges", List.of()));
            }

            List<Map<String, Object>> nodes = graph.getNodes().stream()
                    .filter(n -> n != null && n.getId() != null)
                    .map(n -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", n.getId());
                        m.put("label", n.getLabel() != null ? n.getLabel() : n.getId());
                        m.put("type", n.getType() != null ? n.getType().toUpperCase() : "UNKNOWN");
                        m.put("layer", n.getLayer());
                        return m;
                    }).collect(Collectors.toList());

            List<Map<String, Object>> edges = graph.getEdges().stream()
                    .filter(e -> e != null && e.getFrom() != null && e.getTo() != null)
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("from", e.getFrom());
                        m.put("to", e.getTo());
                        m.put("relation", e.getRelation() != null ? e.getRelation() : "DEPENDS_ON");
                        return m;
                    }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("nodes", nodes, "edges", edges));
        } catch (Exception e) {
            log.error("[CallGraph] Failed for repositoryId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("nodes", List.of(), "edges", List.of()));
        }
    }
}
