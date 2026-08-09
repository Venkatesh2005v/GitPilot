package com.example.gitpilot.architecture.callgraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallGraphResponseDto {
    private Long repositoryId;
    private String repositoryName;
    private int totalNodes;
    private int maxDepth;
    private List<ExecutionPathNodeDto> executionPaths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionPathNodeDto {
        private String nodeKey;
        private String name;
        private String category;
        private String layerCategory; // CONTROLLER, SERVICE, REPOSITORY, DATABASE
        private String description;
        private Integer complexityScore;
        @Builder.Default
        private List<ExecutionPathNodeDto> callees = new ArrayList<>();
    }
}
