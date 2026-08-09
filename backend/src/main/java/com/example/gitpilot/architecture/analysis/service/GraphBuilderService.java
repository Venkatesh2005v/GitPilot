package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.GraphResponse;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphNode;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphEdge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class GraphBuilderService {

    public GraphResponse merge(ModuleAnalysisService.ModuleResult moduleResult,
                               ImportAnalysisService.ImportResult importResult,
                               FrameworkDetectionService.DetectedFramework framework) {
        Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
        List<GraphEdge> allEdges = new ArrayList<>();

        // Add module-based nodes first
        if (moduleResult != null && moduleResult.nodes() != null) {
            for (GraphNode n : moduleResult.nodes()) {
                if (n != null && n.getId() != null) nodeMap.put(n.getId(), n);
            }
        }
        // Add file-based nodes
        if (importResult != null && importResult.nodes() != null) {
            for (GraphNode n : importResult.nodes()) {
                if (n != null && n.getId() != null) nodeMap.putIfAbsent(n.getId(), n);
            }
        }

        // Merge edges, deduplicate
        Set<String> edgeKeys = new HashSet<>();
        if (moduleResult != null && moduleResult.edges() != null) {
            for (GraphEdge e : moduleResult.edges()) {
                if (e == null || e.getFrom() == null || e.getTo() == null) continue;
                String key = e.getFrom() + "->" + e.getTo();
                if (edgeKeys.add(key) && nodeMap.containsKey(e.getFrom()) && nodeMap.containsKey(e.getTo())) {
                    allEdges.add(e);
                }
            }
        }
        if (importResult != null && importResult.edges() != null) {
            for (GraphEdge e : importResult.edges()) {
                if (e == null || e.getFrom() == null || e.getTo() == null) continue;
                String key = e.getFrom() + "->" + e.getTo();
                if (edgeKeys.add(key) && nodeMap.containsKey(e.getFrom()) && nodeMap.containsKey(e.getTo())) {
                    allEdges.add(e);
                }
            }
        }

        // Add framework technology node if meaningful
        if (framework != null && framework.name() != null && !"Generic".equals(framework.name()) && !"Unknown".equals(framework.name())) {
            String techId = framework.name().replace(" ", "");
            nodeMap.putIfAbsent(techId, GraphNode.builder()
                    .id(techId)
                    .label(framework.name())
                    .type("Technology")
                    .layer(3)
                    .build());
        }

        log.info("[GraphBuilder] Final graph: {} nodes, {} edges (framework={})",
                nodeMap.size(), allEdges.size(), framework != null ? framework.name() : "none");

        return GraphResponse.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .edges(allEdges)
                .build();
    }
}
