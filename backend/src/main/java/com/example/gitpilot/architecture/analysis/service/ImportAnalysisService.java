package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphNode;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphEdge;
import com.example.gitpilot.architecture.analysis.dto.RepositoryStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class ImportAnalysisService {

    // Suffix → (type, layer)
    private static final List<String[]> SUFFIX_RULES = List.of(
            new String[]{"Controller", "CONTROLLER", "0"},
            new String[]{"Resource", "CONTROLLER", "0"},
            new String[]{"Router", "CONTROLLER", "0"},
            new String[]{"Route", "CONTROLLER", "0"},
            new String[]{"Endpoint", "CONTROLLER", "0"},
            new String[]{"Page", "PAGE", "0"},
            new String[]{"Screen", "PAGE", "0"},
            new String[]{"View", "PAGE", "0"},
            new String[]{"Service", "SERVICE", "1"},
            new String[]{"UseCase", "SERVICE", "1"},
            new String[]{"Handler", "SERVICE", "1"},
            new String[]{"Middleware", "SERVICE", "1"},
            new String[]{"Provider", "PROVIDER", "1"},
            new String[]{"Hook", "HOOK", "1"},
            new String[]{"Bloc", "SERVICE", "1"},
            new String[]{"Component", "COMPONENT", "1"},
            new String[]{"Widget", "COMPONENT", "1"},
            new String[]{"Repository", "REPOSITORY", "2"},
            new String[]{"Repo", "REPOSITORY", "2"},
            new String[]{"Dao", "REPOSITORY", "2"},
            new String[]{"Client", "API", "2"},
            new String[]{"Gateway", "API", "2"},
            new String[]{"Model", "MODEL", "3"},
            new String[]{"Entity", "MODEL", "3"},
            new String[]{"Dto", "DTO", "3"},
            new String[]{"Schema", "MODEL", "3"},
            new String[]{"Config", "UTILITY", "4"},
            new String[]{"Configuration", "UTILITY", "4"},
            new String[]{"Utils", "UTILITY", "4"},
            new String[]{"Helper", "UTILITY", "4"}
    );

    public record ImportResult(List<GraphNode> nodes, List<GraphEdge> edges) {}

    public ImportResult analyze(RepositoryStructure structure) {
        Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
        List<GraphEdge> edges = new ArrayList<>();

        // 1. Classify files by naming convention
        for (String file : structure.getFiles()) {
            String fileName = file.contains("/") ? file.substring(file.lastIndexOf('/') + 1) : file;
            if (!isSourceFile(fileName)) continue;
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

            for (String[] rule : SUFFIX_RULES) {
                if (baseName.endsWith(rule[0]) || baseName.endsWith(rule[0].toLowerCase())) {
                    nodeMap.putIfAbsent(baseName, GraphNode.builder()
                            .id(baseName).label(baseName).type(rule[1]).layer(Integer.parseInt(rule[2])).build());
                    break;
                }
            }
        }

        // 2. Build dependency edges by prefix matching (domain grouping)
        List<GraphNode> allNodes = new ArrayList<>(nodeMap.values());
        Map<String, List<GraphNode>> byType = new HashMap<>();
        for (GraphNode n : allNodes) {
            byType.computeIfAbsent(n.getType(), k -> new ArrayList<>()).add(n);
        }

        List<GraphNode> controllers = byType.getOrDefault("CONTROLLER", List.of());
        List<GraphNode> services = byType.getOrDefault("SERVICE", List.of());
        List<GraphNode> providers = byType.getOrDefault("PROVIDER", List.of());
        List<GraphNode> repositories = byType.getOrDefault("REPOSITORY", List.of());
        List<GraphNode> apis = byType.getOrDefault("API", List.of());
        List<GraphNode> models = byType.getOrDefault("MODEL", List.of());
        List<GraphNode> pages = byType.getOrDefault("PAGE", List.of());
        List<GraphNode> hooks = byType.getOrDefault("HOOK", List.of());
        List<GraphNode> components = byType.getOrDefault("COMPONENT", List.of());

        // Controller/Route → Service (by domain prefix)
        linkByPrefix(controllers, services, "DEPENDS_ON", edges);
        // Page/Screen → Service/Hook/Provider
        linkByPrefix(pages, services, "CALLS", edges);
        linkByPrefix(pages, hooks, "CALLS", edges);
        linkByPrefix(pages, providers, "CALLS", edges);
        // Component → Hook/Service
        linkByPrefix(components, hooks, "CALLS", edges);
        linkByPrefix(components, services, "CALLS", edges);
        // Service → Repository/API/Client
        linkByPrefix(services, repositories, "DEPENDS_ON", edges);
        linkByPrefix(services, apis, "DEPENDS_ON", edges);
        // Provider → Service/Repository
        linkByPrefix(providers, services, "DEPENDS_ON", edges);
        linkByPrefix(providers, repositories, "DEPENDS_ON", edges);
        // Hook → Service/API
        linkByPrefix(hooks, services, "CALLS", edges);
        linkByPrefix(hooks, apis, "CALLS", edges);
        // Repository → Model
        linkByPrefix(repositories, models, "USES", edges);

        // Fallback: if controllers have no edges, connect to first available service
        Set<String> sourcesWithEdges = new HashSet<>();
        for (GraphEdge e : edges) sourcesWithEdges.add(e.getFrom());
        for (GraphNode ctrl : controllers) {
            if (!sourcesWithEdges.contains(ctrl.getId()) && !services.isEmpty()) {
                edges.add(GraphEdge.builder().from(ctrl.getId()).to(services.get(0).getId()).relation("CALLS").build());
            }
        }

        log.info("[ImportAnalysis] nodes={} edges={} from {} source files", nodeMap.size(), edges.size(), structure.getFiles().size());
        return new ImportResult(new ArrayList<>(nodeMap.values()), edges);
    }

    private void linkByPrefix(List<GraphNode> sources, List<GraphNode> targets, String relation, List<GraphEdge> edges) {
        for (GraphNode src : sources) {
            String prefix = extractDomainPrefix(src.getId()).toLowerCase();
            if (prefix.isEmpty()) continue;
            for (GraphNode tgt : targets) {
                if (tgt.getId().toLowerCase().contains(prefix) && !src.getId().equals(tgt.getId())) {
                    edges.add(GraphEdge.builder().from(src.getId()).to(tgt.getId()).relation(relation).build());
                }
            }
        }
    }

    private String extractDomainPrefix(String name) {
        for (String[] rule : SUFFIX_RULES) {
            if (name.endsWith(rule[0])) return name.substring(0, name.length() - rule[0].length());
            if (name.endsWith(rule[0].toLowerCase())) return name.substring(0, name.length() - rule[0].length());
        }
        return name;
    }

    private boolean isSourceFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".py")
                || lower.endsWith(".js") || lower.endsWith(".jsx") || lower.endsWith(".ts")
                || lower.endsWith(".tsx") || lower.endsWith(".go") || lower.endsWith(".rs")
                || lower.endsWith(".dart") || lower.endsWith(".php") || lower.endsWith(".rb")
                || lower.endsWith(".cs") || lower.endsWith(".swift") || lower.endsWith(".vue");
    }
}
