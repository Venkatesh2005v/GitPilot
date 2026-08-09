package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.GraphResponse;
import com.example.gitpilot.architecture.analysis.dto.RepositoryStructure;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryArchitectureService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryScannerService scannerService;
    private final FrameworkDetectionService frameworkDetectionService;
    private final ModuleAnalysisService moduleAnalysisService;
    private final ImportAnalysisService importAnalysisService;
    private final GraphBuilderService graphBuilderService;

    public GraphResponse analyzeRepository(Long repositoryId, String accessToken) {
        log.info("[ArchPipeline] === Starting for repositoryId={} token={} ===", repositoryId, accessToken != null);
        long pipelineStart = System.currentTimeMillis();

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        if (accessToken == null) {
            log.warn("[ArchPipeline] No access token. Returning empty graph.");
            return GraphResponse.builder().build();
        }

        // Extract owner/repo
        String owner = "";
        String repo = "";
        if (repository.getHtmlUrl() != null && repository.getHtmlUrl().contains("github.com/")) {
            String path = repository.getHtmlUrl().substring(repository.getHtmlUrl().indexOf("github.com/") + 11);
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
            String[] parts = path.split("/");
            if (parts.length >= 2) { owner = parts[0]; repo = parts[1]; }
        }

        if (owner.isEmpty() || repo.isEmpty()) {
            log.warn("[ArchPipeline] Cannot resolve owner/repo from htmlUrl={}. Returning empty graph.", repository.getHtmlUrl());
            return GraphResponse.builder().build();
        }

        log.info("[ArchPipeline] Resolved: owner={} repo={}", owner, repo);

        // STEP 1
        long t1 = System.currentTimeMillis();
        RepositoryStructure structure;
        try {
            structure = scannerService.scan(owner, repo, accessToken);
            if (structure == null) {
                structure = RepositoryStructure.builder().build();
            }
            log.info("[ArchPipeline] STEP 1 RepositoryScannerService COMPLETE: files={} folders={} extensions={} manifests={} ({}ms)",
                    structure.getFiles().size(), structure.getFolders().size(), structure.getExtensions().size(), structure.getManifestFiles().size(), System.currentTimeMillis() - t1);
        } catch (Exception e) {
            log.error("[ArchPipeline] STEP 1 RepositoryScannerService FAILED", e);
            return GraphResponse.builder().build();
        }

        // STEP 2
        long t2 = System.currentTimeMillis();
        FrameworkDetectionService.DetectedFramework framework;
        try {
            framework = frameworkDetectionService.detect(structure);
            if (framework == null) {
                framework = new FrameworkDetectionService.DetectedFramework("Unknown", "Unknown", "Unknown");
            }
            log.info("[ArchPipeline] STEP 2 FrameworkDetectionService COMPLETE: framework={} language={} arch={} ({}ms)",
                    framework.name(), framework.language(), framework.architecture(), System.currentTimeMillis() - t2);
        } catch (Exception e) {
            log.error("[ArchPipeline] STEP 2 FrameworkDetectionService FAILED", e);
            framework = new FrameworkDetectionService.DetectedFramework("Unknown", "Unknown", "Unknown");
        }

        // STEP 3
        long t3 = System.currentTimeMillis();
        ModuleAnalysisService.ModuleResult moduleResult;
        try {
            moduleResult = moduleAnalysisService.analyze(structure);
            if (moduleResult == null) {
                moduleResult = new ModuleAnalysisService.ModuleResult(java.util.List.of(), java.util.List.of());
            }
            log.info("[ArchPipeline] STEP 3 ModuleAnalysisService COMPLETE: nodes={} edges={} ({}ms)",
                    moduleResult.nodes().size(), moduleResult.edges().size(), System.currentTimeMillis() - t3);
        } catch (Exception e) {
            log.error("[ArchPipeline] STEP 3 ModuleAnalysisService FAILED", e);
            moduleResult = new ModuleAnalysisService.ModuleResult(java.util.List.of(), java.util.List.of());
        }

        // STEP 4
        long t4 = System.currentTimeMillis();
        ImportAnalysisService.ImportResult importResult;
        try {
            importResult = importAnalysisService.analyze(structure);
            if (importResult == null) {
                importResult = new ImportAnalysisService.ImportResult(java.util.List.of(), java.util.List.of());
            }
            log.info("[ArchPipeline] STEP 4 ImportAnalysisService COMPLETE: nodes={} edges={} ({}ms)",
                    importResult.nodes().size(), importResult.edges().size(), System.currentTimeMillis() - t4);
        } catch (Exception e) {
            log.error("[ArchPipeline] STEP 4 ImportAnalysisService FAILED", e);
            importResult = new ImportAnalysisService.ImportResult(java.util.List.of(), java.util.List.of());
        }

        // STEP 5
        long t5 = System.currentTimeMillis();
        GraphResponse graph;
        try {
            graph = graphBuilderService.merge(moduleResult, importResult, framework);
            if (graph == null) {
                graph = GraphResponse.builder().build();
            }
            log.info("[ArchPipeline] STEP 5 GraphBuilderService COMPLETE: nodes={} edges={} ({}ms)",
                    graph.getNodes().size(), graph.getEdges().size(), System.currentTimeMillis() - t5);
        } catch (Exception e) {
            log.error("[ArchPipeline] STEP 5 GraphBuilderService FAILED", e);
            graph = GraphResponse.builder().build();
        }

        log.info("[ArchPipeline] === DONE for repositoryId={} totalTime={}ms nodes={} edges={} ===",
                repositoryId, System.currentTimeMillis() - pipelineStart, graph.getNodes().size(), graph.getEdges().size());

        return graph;
    }
}
