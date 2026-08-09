package com.example.gitpilot.architecture.apiflow.service;

import com.example.gitpilot.architecture.apiflow.dto.ApiFlowPathDto;
import com.example.gitpilot.architecture.apiflow.dto.ApiFlowPathDto.ApiFlowStepDto;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphEdge;
import com.example.gitpilot.architecture.analysis.dto.GraphResponse.GraphNode;
import com.example.gitpilot.architecture.analysis.service.RepositoryArchitectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiFlowService {

    private final RepositoryArchitectureService architectureService;

    public List<ApiFlowPathDto> discoverApiFlows(Long repositoryId, String accessToken) {
        log.info("[ApiFlow] repositoryId={} accessTokenPresent={}", repositoryId, accessToken != null);

        GraphResponse graph = architectureService.analyzeRepository(repositoryId, accessToken);
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            log.info("[ApiFlow] No architecture data for repositoryId={}", repositoryId);
            return List.of();
        }

        // Build lookup maps
        Map<String, GraphNode> nodeMap = new HashMap<>();
        for (GraphNode n : graph.getNodes()) {
            if (n != null && n.getId() != null) nodeMap.put(n.getId(), n);
        }

        Map<String, List<String>> outgoing = new HashMap<>();
        if (graph.getEdges() != null) {
            for (GraphEdge e : graph.getEdges()) {
                if (e != null && e.getFrom() != null && e.getTo() != null) {
                    outgoing.computeIfAbsent(e.getFrom(), k -> new ArrayList<>()).add(e.getTo());
                }
            }
        }

        // Find controllers and trace their flows
        List<ApiFlowPathDto> flows = new ArrayList<>();
        for (GraphNode node : graph.getNodes()) {
            if (node == null || node.getType() == null) continue;
            String type = node.getType().toUpperCase();
            if (!type.equals("CONTROLLER") && !type.equals("ROUTE")) continue;

            String controllerName = node.getId();
            String endpoint = inferEndpoint(controllerName);
            String httpMethod = "GET";

            // Trace: Controller → Service → Repository
            String serviceName = "—";
            String repoName = "—";
            String dbTable = "—";

            List<String> deps = outgoing.getOrDefault(controllerName, List.of());
            for (String dep : deps) {
                GraphNode depNode = nodeMap.get(dep);
                if (depNode == null) continue;
                String depType = depNode.getType() != null ? depNode.getType().toUpperCase() : "";
                if (depType.equals("SERVICE") || depType.equals("PROVIDER") || depType.equals("HOOK")) {
                    serviceName = depNode.getId();
                    // Look one more level: Service → Repository
                    for (String svcDep : outgoing.getOrDefault(serviceName, List.of())) {
                        GraphNode repoNode = nodeMap.get(svcDep);
                        if (repoNode != null) {
                            String rt = repoNode.getType() != null ? repoNode.getType().toUpperCase() : "";
                            if (rt.equals("REPOSITORY") || rt.equals("API")) {
                                repoName = repoNode.getId();
                                dbTable = repoName.replaceAll("(?i)Repository$", "").toLowerCase() + "s";
                                break;
                            }
                        }
                    }
                    break;
                } else if (depType.equals("REPOSITORY") || depType.equals("API")) {
                    repoName = depNode.getId();
                    dbTable = repoName.replaceAll("(?i)Repository$", "").toLowerCase() + "s";
                }
            }

            // Build steps
            List<ApiFlowStepDto> steps = new ArrayList<>();
            steps.add(ApiFlowStepDto.builder().stepOrder(1).layer("CLIENT")
                    .componentName("HTTP Client").action("Request").details(httpMethod + " " + endpoint).build());
            steps.add(ApiFlowStepDto.builder().stepOrder(2).layer("CONTROLLER")
                    .componentName(controllerName).action("Handle Request").details("Delegates to " + serviceName).build());
            if (!"—".equals(serviceName)) {
                steps.add(ApiFlowStepDto.builder().stepOrder(3).layer("SERVICE")
                        .componentName(serviceName).action("Business Logic").details("Queries " + repoName).build());
            }
            if (!"—".equals(repoName)) {
                steps.add(ApiFlowStepDto.builder().stepOrder(steps.size() + 1).layer("REPOSITORY")
                        .componentName(repoName).action("Data Access").details("Table: " + dbTable).build());
            }

            flows.add(ApiFlowPathDto.builder()
                    .endpoint(endpoint).httpMethod(httpMethod)
                    .controllerClass(controllerName).controllerMethod("handle()")
                    .serviceClass(serviceName).serviceMethod("execute()")
                    .repositoryClass(repoName).databaseTable(dbTable)
                    .flowSteps(steps).build());
        }

        log.info("[ApiFlow] flows generated={} for repositoryId={}", flows.size(), repositoryId);
        return flows;
    }

    private String inferEndpoint(String controllerName) {
        String base = controllerName.replaceAll("(?i)(Controller|Resource|Router|Route|Endpoint)$", "");
        if (base.isEmpty()) return "/api/unknown";
        return "/api/" + base.substring(0, 1).toLowerCase() + base.substring(1);
    }
}
