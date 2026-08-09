package com.example.gitpilot.architecture.techdebt.service;

import com.example.gitpilot.architecture.techdebt.dto.CodeMetricsDto;
import com.example.gitpilot.architecture.techdebt.dto.CodeMetricsDto.RepositoryTechDebtOverviewDto;
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
public class TechnicalDebtService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;

    public RepositoryTechDebtOverviewDto computeTechnicalDebtMetrics(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId)));

        Long actualId = repository.getId();
        List<KnowledgeNode> nodes = nodeRepository.findByRepository(repository);
        List<KnowledgeEdge> edges = edgeRepository.findByRepository(repository);

        Map<String, Integer> fanInMap = new HashMap<>();
        Map<String, Integer> fanOutMap = new HashMap<>();
        Map<String, Integer> inheritanceDepthMap = new HashMap<>();

        for (KnowledgeEdge edge : edges) {
            fanOutMap.put(edge.getSourceNodeKey(), fanOutMap.getOrDefault(edge.getSourceNodeKey(), 0) + 1);
            fanInMap.put(edge.getTargetNodeKey(), fanInMap.getOrDefault(edge.getTargetNodeKey(), 0) + 1);

            if ("INHERITS".equalsIgnoreCase(edge.getRelationshipType())) {
                inheritanceDepthMap.put(edge.getSourceNodeKey(), inheritanceDepthMap.getOrDefault(edge.getTargetNodeKey(), 0) + 1);
            }
        }

        List<CodeMetricsDto> metricsList = new ArrayList<>();
        double totalComplexity = 0;
        double totalInstability = 0;
        int maxDebt = 0;
        String highestDebtClass = "None";

        for (KnowledgeNode node : nodes) {
            if ("METHOD".equalsIgnoreCase(node.getCategory())) {
                continue; // Focus on class level metrics
            }

            int fanIn = fanInMap.getOrDefault(node.getNodeKey(), 0);
            int fanOut = fanOutMap.getOrDefault(node.getNodeKey(), 0);
            int totalConnections = fanIn + fanOut;
            double instability = totalConnections == 0 ? 0.0 : (double) fanOut / totalConnections;
            int inheritanceDepth = inheritanceDepthMap.getOrDefault(node.getNodeKey(), 1);

            int loc = extractLocFromDescription(node.getDescription());
            int complexity = node.getComplexityScore() != null ? node.getComplexityScore() / 5 : 5;

            int techDebtScore = computeScore(loc, complexity, fanIn, fanOut, instability);
            if (techDebtScore > maxDebt) {
                maxDebt = techDebtScore;
                highestDebtClass = node.getName();
            }

            String rec = generateRecommendation(techDebtScore, instability, fanOut, complexity);

            CodeMetricsDto metrics = CodeMetricsDto.builder()
                    .className(node.getName())
                    .nodeKey(node.getNodeKey())
                    .category(node.getCategory())
                    .loc(loc)
                    .cyclomaticComplexity(complexity)
                    .fanIn(fanIn)
                    .fanOut(fanOut)
                    .instabilityIndex(Math.round(instability * 100.0) / 100.0)
                    .inheritanceDepth(inheritanceDepth)
                    .techDebtScore(techDebtScore)
                    .refactoringRecommendation(rec)
                    .build();

            metricsList.add(metrics);
            totalComplexity += complexity;
            totalInstability += instability;
        }

        int avgComp = metricsList.isEmpty() ? 0 : (int) (totalComplexity / metricsList.size());
        double avgInst = metricsList.isEmpty() ? 0.0 : Math.round((totalInstability / metricsList.size()) * 100.0) / 100.0;

        return RepositoryTechDebtOverviewDto.builder()
                .repositoryId(actualId)
                .averageComplexity(avgComp)
                .averageInstability(avgInst)
                .highestTechDebtScore(maxDebt)
                .highestDebtClass(highestDebtClass)
                .classMetrics(metricsList)
                .build();
    }

    private int extractLocFromDescription(String desc) {
        if (desc != null && desc.contains("LOC: ")) {
            try {
                String sub = desc.substring(desc.indexOf("LOC: ") + 5);
                if (sub.contains(" |")) sub = sub.substring(0, sub.indexOf(" |"));
                return Integer.parseInt(sub.trim());
            } catch (Exception ignored) {}
        }
        return 45;
    }

    private int computeScore(int loc, int complexity, int fanIn, int fanOut, double instability) {
        int score = 10;
        if (loc > 300) score += 25;
        if (complexity > 15) score += 30;
        if (fanOut > 8) score += 20;
        if (instability > 0.8 && fanIn > 5) score += 15; // Fragile class
        return Math.min(100, score);
    }

    private String generateRecommendation(int score, double instability, int fanOut, int complexity) {
        if (score > 70) {
            return "High technical debt! Extract cohesive helper interfaces and split monolithic methods to reduce cyclomatic complexity.";
        }
        if (instability > 0.8 && fanOut > 6) {
            return "High instability index. Class depends on too many volatile concrete implementations; introduce interface abstraction boundaries.";
        }
        if (complexity > 15) {
            return "Consider reducing conditional branching logic through Strategy or Command pattern refactoring.";
        }
        return "Clean code metrics. Architecture maintains good modular stability.";
    }
}
