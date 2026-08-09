package com.example.gitpilot.architecture.drift.service;

import com.example.gitpilot.architecture.drift.dto.DriftViolationDto;
import com.example.gitpilot.architecture.drift.dto.DriftViolationDto.DriftReportSummaryDto;
import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.memory.repository.KnowledgeEdgeRepository;
import com.example.gitpilot.memory.repository.KnowledgeNodeRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureDriftService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;

    public DriftReportSummaryDto detectDrift(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId)));

        Long actualId = repository.getId();
        List<KnowledgeNode> nodes = nodeRepository.findByRepository(repository);
        List<KnowledgeEdge> edges = edgeRepository.findByRepository(repository);

        List<DriftViolationDto> violations = new ArrayList<>();

        Map<String, KnowledgeNode> nodeMap = new HashMap<>();
        for (KnowledgeNode n : nodes) {
            nodeMap.put(n.getNodeKey(), n);
        }

        // Rule 1: Controller -> Repository direct access (bypassing Service)
        for (KnowledgeEdge edge : edges) {
            KnowledgeNode source = nodeMap.get(edge.getSourceNodeKey());
            KnowledgeNode target = nodeMap.get(edge.getTargetNodeKey());

            if (source != null && target != null) {
                if ("CONTROLLER".equalsIgnoreCase(source.getCategory()) && "REPOSITORY".equalsIgnoreCase(target.getCategory())) {
                    violations.add(DriftViolationDto.builder()
                            .violationType("CONTROLLER_DIRECT_REPOSITORY_ACCESS")
                            .severity("HIGH")
                            .sourceComponent(source.getName())
                            .targetComponent(target.getName())
                            .ruleName("Strict Layering Rule")
                            .description(String.format("%s directly accesses %s, bypassing the Service layer business logic boundary.", source.getName(), target.getName()))
                            .recommendation(String.format("Refactor %s to interact with a Service class instead of directly calling %s.", source.getName(), target.getName()))
                            .aiExplanation(String.format("Deterministic Violation Detected: %s invokes %s directly. Controllers should only handle HTTP concerns and delegate domain orchestrations to Services.", source.getName(), target.getName()))
                            .build());
                }
            }
        }

        // Rule 2: God Class Detection (High complexity score or excessive connections)
        for (KnowledgeNode node : nodes) {
            if (node.getComplexityScore() != null && node.getComplexityScore() > 60) {
                violations.add(DriftViolationDto.builder()
                        .violationType("GOD_CLASS")
                        .severity(node.getComplexityScore() > 80 ? "CRITICAL" : "MEDIUM")
                        .sourceComponent(node.getName())
                        .targetComponent("System Module")
                        .ruleName("Single Responsibility Principle (SRP)")
                        .description(String.format("%s has a complexity score of %d, indicating excessive responsibilities and high churn risk.", node.getName(), node.getComplexityScore()))
                        .recommendation(String.format("Decompose %s into smaller domain services or value objects.", node.getName()))
                        .aiExplanation(String.format("Deterministic Violation Detected: %s exhibits high cyclomatic complexity and large structural surface area.", node.getName()))
                        .build());
            }
        }

        // Rule 3: Circular Dependency Check
        Map<String, List<String>> adj = new HashMap<>();
        for (KnowledgeEdge edge : edges) {
            adj.computeIfAbsent(edge.getSourceNodeKey(), k -> new ArrayList<>()).add(edge.getTargetNodeKey());
        }
        detectCircularDependencies(nodes, adj, violations, nodeMap);

        // Compute summary metrics
        int total = violations.size();
        int critical = (int) violations.stream().filter(v -> "CRITICAL".equalsIgnoreCase(v.getSeverity())).count();
        int high = (int) violations.stream().filter(v -> "HIGH".equalsIgnoreCase(v.getSeverity())).count();
        int medium = (int) violations.stream().filter(v -> "MEDIUM".equalsIgnoreCase(v.getSeverity())).count();
        int healthScore = Math.max(0, 100 - (critical * 25 + high * 15 + medium * 5));

        return DriftReportSummaryDto.builder()
                .repositoryId(actualId)
                .totalViolations(total)
                .criticalViolations(critical)
                .highViolations(high)
                .mediumViolations(medium)
                .architecturalHealthScore(healthScore)
                .violations(violations)
                .build();
    }

    private void detectCircularDependencies(List<KnowledgeNode> nodes, Map<String, List<String>> adj, List<DriftViolationDto> violations, Map<String, KnowledgeNode> nodeMap) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (KnowledgeNode node : nodes) {
            if (!visited.contains(node.getNodeKey())) {
                findCyclesDFS(node.getNodeKey(), adj, visited, recStack, new ArrayList<>(), violations, nodeMap);
            }
        }
    }

    private void findCyclesDFS(String current, Map<String, List<String>> adj, Set<String> visited, Set<String> recStack, List<String> path, List<DriftViolationDto> violations, Map<String, KnowledgeNode> nodeMap) {
        visited.add(current);
        recStack.add(current);
        path.add(current);

        List<String> neighbors = adj.getOrDefault(current, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                findCyclesDFS(neighbor, adj, visited, recStack, path, violations, nodeMap);
            } else if (recStack.contains(neighbor)) {
                // Cycle detected
                KnowledgeNode src = nodeMap.get(current);
                KnowledgeNode tgt = nodeMap.get(neighbor);
                String srcName = src != null ? src.getName() : current;
                String tgtName = tgt != null ? tgt.getName() : neighbor;

                violations.add(DriftViolationDto.builder()
                        .violationType("CIRCULAR_DEPENDENCY")
                        .severity("CRITICAL")
                        .sourceComponent(srcName)
                        .targetComponent(tgtName)
                        .ruleName("Acyclic Dependencies Principle (ADP)")
                        .description(String.format("Circular dependency path detected between %s and %s.", srcName, tgtName))
                        .recommendation("Break the circular cycle by introducing interface abstractions or event publishing.")
                        .aiExplanation(String.format("Deterministic Violation Detected: Cycle found in graph traversal: %s -> %s.", srcName, tgtName))
                        .build());
            }
        }

        recStack.remove(current);
        if (!path.isEmpty()) path.remove(path.size() - 1);
    }
}
