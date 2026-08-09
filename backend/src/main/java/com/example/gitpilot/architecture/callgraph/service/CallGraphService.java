package com.example.gitpilot.architecture.callgraph.service;

import com.example.gitpilot.architecture.callgraph.dto.CallGraphResponseDto;
import com.example.gitpilot.architecture.callgraph.dto.CallGraphResponseDto.ExecutionPathNodeDto;
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
public class CallGraphService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;

    public CallGraphResponseDto generateCallGraph(Long repositoryId, String rootKey) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId)));

        Long actualId = repository.getId();
        List<KnowledgeNode> allNodes = nodeRepository.findByRepository(repository);
        List<KnowledgeEdge> allEdges = edgeRepository.findByRepository(repository);

        Map<String, KnowledgeNode> nodeMap = new HashMap<>();
        for (KnowledgeNode node : allNodes) {
            nodeMap.put(node.getNodeKey(), node);
        }

        Map<String, List<String>> adjList = new HashMap<>();
        for (KnowledgeEdge edge : allEdges) {
            adjList.computeIfAbsent(edge.getSourceNodeKey(), k -> new ArrayList<>()).add(edge.getTargetNodeKey());
        }

        List<ExecutionPathNodeDto> rootPaths = new ArrayList<>();
        List<KnowledgeNode> startNodes = new ArrayList<>();

        if (rootKey != null && !rootKey.isBlank() && nodeMap.containsKey(rootKey)) {
            startNodes.add(nodeMap.get(rootKey));
        } else {
            // Find all CONTROLLER or Entry Point nodes
            for (KnowledgeNode node : allNodes) {
                if ("CONTROLLER".equalsIgnoreCase(node.getCategory()) || "REST_ENDPOINT".equalsIgnoreCase(node.getCategory())) {
                    startNodes.add(node);
                }
            }
            if (startNodes.isEmpty() && !allNodes.isEmpty()) {
                startNodes.add(allNodes.get(0));
            }
        }

        Set<String> visited = new HashSet<>();
        for (KnowledgeNode startNode : startNodes) {
            ExecutionPathNodeDto rootDto = buildExecutionTree(startNode, nodeMap, adjList, visited, 0, 5);
            rootPaths.add(rootDto);
        }

        return CallGraphResponseDto.builder()
                .repositoryId(actualId)
                .repositoryName(repository.getName())
                .totalNodes(allNodes.size())
                .maxDepth(4)
                .executionPaths(rootPaths)
                .build();
    }

    private ExecutionPathNodeDto buildExecutionTree(KnowledgeNode currentNode,
                                                     Map<String, KnowledgeNode> nodeMap,
                                                     Map<String, List<String>> adjList,
                                                     Set<String> visited,
                                                     int depth,
                                                     int maxDepth) {
        ExecutionPathNodeDto dto = ExecutionPathNodeDto.builder()
                .nodeKey(currentNode.getNodeKey())
                .name(currentNode.getName())
                .category(currentNode.getCategory())
                .layerCategory(determineLayerCategory(currentNode))
                .description(currentNode.getDescription())
                .complexityScore(currentNode.getComplexityScore())
                .callees(new ArrayList<>())
                .build();

        if (depth >= maxDepth || visited.contains(currentNode.getNodeKey())) {
            return dto;
        }

        visited.add(currentNode.getNodeKey());
        List<String> neighbors = adjList.getOrDefault(currentNode.getNodeKey(), Collections.emptyList());

        for (String targetKey : neighbors) {
            KnowledgeNode childNode = nodeMap.get(targetKey);
            if (childNode != null) {
                ExecutionPathNodeDto childDto = buildExecutionTree(childNode, nodeMap, adjList, new HashSet<>(visited), depth + 1, maxDepth);
                dto.getCallees().add(childDto);
            }
        }

        // Add implicit database layer if repository node
        if ("REPOSITORY".equalsIgnoreCase(currentNode.getCategory()) && dto.getCallees().isEmpty()) {
            dto.getCallees().add(ExecutionPathNodeDto.builder()
                    .nodeKey(currentNode.getNodeKey() + "#PostgreSQL")
                    .name("PostgreSQL Database")
                    .category("DATABASE")
                    .layerCategory("DATABASE")
                    .description("Persistent database storage table for " + currentNode.getName())
                    .complexityScore(10)
                    .callees(Collections.emptyList())
                    .build());
        }

        return dto;
    }

    private String determineLayerCategory(KnowledgeNode node) {
        if (node.getCategory() != null) {
            String cat = node.getCategory().toUpperCase();
            if (cat.contains("CONTROLLER")) return "CONTROLLER";
            if (cat.contains("SERVICE")) return "SERVICE";
            if (cat.contains("REPOSITORY") || cat.contains("DAO")) return "REPOSITORY";
            if (cat.contains("ENTITY") || cat.contains("TABLE") || cat.contains("DATABASE")) return "DATABASE";
        }
        return "SERVICE";
    }
}
