package com.example.gitpilot.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMapDto {
    private Long repositoryId;
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDto {
        private Long id;
        private String nodeKey;
        private String name;
        private String category;
        private String description;
        private Integer complexityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDto {
        private Long id;
        private String sourceNodeKey;
        private String targetNodeKey;
        private String relationshipType;
        private String label;
    }
}
