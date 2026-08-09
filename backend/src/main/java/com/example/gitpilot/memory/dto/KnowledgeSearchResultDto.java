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
public class KnowledgeSearchResultDto {
    private String query;
    private int totalMatches;
    private List<SearchResultItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        private String type; // DECISION, ONBOARDING, MILESTONE, KNOWLEDGE_NODE
        private String title;
        private String snippet;
        private String category;
        private String linkUrl;
        private double matchScore;
    }
}
