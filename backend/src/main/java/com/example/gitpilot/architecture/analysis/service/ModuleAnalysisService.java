package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphNode;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphEdge;
import com.example.gitpilot.architecture.analysis.dto.RepositoryStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ModuleAnalysisService {

    // folder name -> (node type, layer)
    private static final Map<String, String[]> FOLDER_CLASSIFICATION = Map.ofEntries(
            Map.entry("controllers", new String[]{"Controller", "0"}),
            Map.entry("controller", new String[]{"Controller", "0"}),
            Map.entry("routes", new String[]{"Controller", "0"}),
            Map.entry("api", new String[]{"Controller", "0"}),
            Map.entry("pages", new String[]{"Component", "0"}),
            Map.entry("views", new String[]{"Component", "0"}),
            Map.entry("screens", new String[]{"Component", "0"}),
            Map.entry("services", new String[]{"Service", "1"}),
            Map.entry("service", new String[]{"Service", "1"}),
            Map.entry("usecases", new String[]{"Service", "1"}),
            Map.entry("handlers", new String[]{"Service", "1"}),
            Map.entry("middleware", new String[]{"Service", "1"}),
            Map.entry("components", new String[]{"Component", "1"}),
            Map.entry("hooks", new String[]{"Service", "1"}),
            Map.entry("repositories", new String[]{"Repository", "2"}),
            Map.entry("repository", new String[]{"Repository", "2"}),
            Map.entry("models", new String[]{"Model", "2"}),
            Map.entry("entities", new String[]{"Model", "2"}),
            Map.entry("schemas", new String[]{"Model", "2"}),
            Map.entry("database", new String[]{"Repository", "2"}),
            Map.entry("db", new String[]{"Repository", "2"}),
            Map.entry("config", new String[]{"Configuration", "3"}),
            Map.entry("configuration", new String[]{"Configuration", "3"}),
            Map.entry("utils", new String[]{"Service", "3"}),
            Map.entry("helpers", new String[]{"Service", "3"}),
            Map.entry("dto", new String[]{"Model", "3"}),
            Map.entry("types", new String[]{"Model", "3"})
    );

    public record ModuleResult(List<GraphNode> nodes, List<GraphEdge> edges) {}

    public ModuleResult analyze(RepositoryStructure structure) {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (String folder : structure.getFolders()) {
            String name = folder.contains("/") ? folder.substring(folder.lastIndexOf('/') + 1) : folder;
            String lower = name.toLowerCase();

            String[] classification = FOLDER_CLASSIFICATION.get(lower);
            if (classification != null) {
                String nodeId = capitalize(name);
                if (added.add(nodeId)) {
                    nodes.add(GraphNode.builder()
                            .id(nodeId)
                            .label(nodeId)
                            .type(classification[0])
                            .layer(Integer.parseInt(classification[1]))
                            .build());
                }
            }
        }

        // Infer standard architecture edges
        // Controllers -> Services
        nodes.stream().filter(n -> "Controller".equals(n.getType())).forEach(ctrl ->
            nodes.stream().filter(n -> "Service".equals(n.getType())).forEach(svc ->
                edges.add(GraphEdge.builder().from(ctrl.getId()).to(svc.getId()).relation("calls").build())
            )
        );
        // Services -> Repositories
        nodes.stream().filter(n -> "Service".equals(n.getType())).forEach(svc ->
            nodes.stream().filter(n -> "Repository".equals(n.getType())).forEach(repo ->
                edges.add(GraphEdge.builder().from(svc.getId()).to(repo.getId()).relation("uses").build())
            )
        );
        // Services -> Models
        nodes.stream().filter(n -> "Service".equals(n.getType())).forEach(svc ->
            nodes.stream().filter(n -> "Model".equals(n.getType())).forEach(model ->
                edges.add(GraphEdge.builder().from(svc.getId()).to(model.getId()).relation("depends_on").build())
            )
        );

        log.info("[ModuleAnalysis] Found {} modules, {} edges", nodes.size(), edges.size());
        return new ModuleResult(nodes, edges);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
