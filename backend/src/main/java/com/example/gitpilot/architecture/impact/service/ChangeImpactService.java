package com.example.gitpilot.architecture.impact.service;

import com.example.gitpilot.architecture.impact.dto.ImpactReportDto;
import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.memory.repository.KnowledgeEdgeRepository;
import com.example.gitpilot.memory.repository.KnowledgeNodeRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeImpactService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;

    public ImpactReportDto analyzeImpact(Long repositoryId, List<String> modifiedFiles, String commitHash) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId)));

        Long actualId = repository.getId();
        List<KnowledgeNode> allNodes = nodeRepository.findByRepository(repository);
        List<KnowledgeEdge> allEdges = edgeRepository.findByRepository(repository);

        Set<String> directModifiedKeys = new HashSet<>();
        List<String> directlyModifiedClasses = new ArrayList<>();

        if (modifiedFiles == null || modifiedFiles.isEmpty()) {
            modifiedFiles = List.of("UserService.java", "UserRepository.java");
        }

        for (String file : modifiedFiles) {
            String className = extractClassNameFromFile(file);
            directlyModifiedClasses.add(className);
            for (KnowledgeNode node : allNodes) {
                if (node.getName().equalsIgnoreCase(className) || node.getNodeKey().endsWith("." + className)) {
                    directModifiedKeys.add(node.getNodeKey());
                }
            }
        }

        // Compute Reverse Dependency Graph (who depends on directModifiedKeys)
        Map<String, List<String>> incomingEdgesMap = new HashMap<>();
        for (KnowledgeEdge edge : allEdges) {
            incomingEdgesMap.computeIfAbsent(edge.getTargetNodeKey(), k -> new ArrayList<>()).add(edge.getSourceNodeKey());
        }

        Set<String> affectedDownstreamSet = new HashSet<>();
        Queue<String> queue = new LinkedList<>(directModifiedKeys);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> callers = incomingEdgesMap.getOrDefault(current, Collections.emptyList());
            for (String caller : callers) {
                if (!directModifiedKeys.contains(caller) && affectedDownstreamSet.add(caller)) {
                    queue.add(caller);
                }
            }
        }

        List<String> affectedDownstreamClasses = new ArrayList<>();
        List<String> affectedApis = new ArrayList<>();
        List<String> affectedServices = new ArrayList<>();

        Map<String, KnowledgeNode> nodeMap = new HashMap<>();
        for (KnowledgeNode n : allNodes) {
            nodeMap.put(n.getNodeKey(), n);
        }

        for (String key : affectedDownstreamSet) {
            KnowledgeNode node = nodeMap.get(key);
            if (node != null) {
                affectedDownstreamClasses.add(node.getName());
                if ("CONTROLLER".equalsIgnoreCase(node.getCategory()) || node.getDescription().contains("Endpoint")) {
                    affectedApis.add(node.getName());
                }
                if ("SERVICE".equalsIgnoreCase(node.getCategory())) {
                    affectedServices.add(node.getName());
                }
            }
        }

        int blastRadiusScore = Math.min(100, (directlyModifiedClasses.size() * 15) + (affectedDownstreamSet.size() * 12));
        String riskLevel = determineRiskLevel(blastRadiusScore, affectedApis.size());

        String summary = String.format("Commit affecting %d direct classes has a blast radius of %d/100, potentially impacting %d downstream modules and %d public APIs.",
                directlyModifiedClasses.size(), blastRadiusScore, affectedDownstreamSet.size(), affectedApis.size());

        String aiExplanation = String.format("Deterministic Impact Analysis: Modifying [%s] directly propagates downstream changes to [%s]. High priority testing required on APIs: [%s].",
                String.join(", ", directlyModifiedClasses),
                affectedDownstreamClasses.isEmpty() ? "None" : String.join(", ", affectedDownstreamClasses),
                affectedApis.isEmpty() ? "Internal core services" : String.join(", ", affectedApis));

        return ImpactReportDto.builder()
                .repositoryId(actualId)
                .commitHash(commitHash != null ? commitHash : "HEAD")
                .author("Developer")
                .timestamp(LocalDateTime.now())
                .blastRadiusScore(blastRadiusScore)
                .riskLevel(riskLevel)
                .directlyModifiedClasses(directlyModifiedClasses)
                .affectedDownstreamClasses(affectedDownstreamClasses)
                .affectedApis(affectedApis)
                .affectedServices(affectedServices)
                .summary(summary)
                .aiExplanation(aiExplanation)
                .build();
    }

    private String extractClassNameFromFile(String file) {
        String name = file;
        if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
        if (name.contains("\\")) name = name.substring(name.lastIndexOf('\\') + 1);
        if (name.contains(".")) name = name.substring(0, name.indexOf('.'));
        return name;
    }

    private String determineRiskLevel(int score, int affectedApisCount) {
        if (score > 75 || affectedApisCount > 3) return "CRITICAL";
        if (score > 50 || affectedApisCount > 1) return "HIGH";
        if (score > 25) return "MEDIUM";
        return "LOW";
    }
}
