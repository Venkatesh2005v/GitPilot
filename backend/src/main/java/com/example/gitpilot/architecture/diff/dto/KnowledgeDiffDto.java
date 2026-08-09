package com.example.gitpilot.architecture.diff.dto;

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
public class KnowledgeDiffDto {
    private String sourceRef; // e.g. commit / branch / tag A
    private String targetRef; // e.g. commit / branch / tag B
    private Long repositoryId;
    private int addedNodesCount;
    private int removedNodesCount;
    private int addedEdgesCount;
    private int removedEdgesCount;
    private int complexityDelta;
    @Builder.Default
    private List<String> addedNodes = new ArrayList<>();
    @Builder.Default
    private List<String> removedNodes = new ArrayList<>();
    @Builder.Default
    private List<String> addedRelationships = new ArrayList<>();
    @Builder.Default
    private List<String> removedRelationships = new ArrayList<>();
    @Builder.Default
    private List<String> technologyDiffs = new ArrayList<>();
    private String architectureEvolutionSummary;
}
