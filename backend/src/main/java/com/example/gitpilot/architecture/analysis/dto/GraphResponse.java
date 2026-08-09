package com.example.gitpilot.architecture.analysis.dto;

import lombok.Builder;
import lombok.Data;
import java.util.*;

@Data
@Builder
public class GraphResponse {
    @Builder.Default private List<GraphNode> nodes = new ArrayList<>();
    @Builder.Default private List<GraphEdge> edges = new ArrayList<>();

    @Data
    @Builder
    public static class GraphNode {
        private String id;
        private String label;
        private String type; // Controller, Service, Repository, Component, Module, Model, Configuration, Technology
        private int layer;   // 0=Controller, 1=Service, 2=Repository, 3=Other
    }

    @Data
    @Builder
    public static class GraphEdge {
        private String from;
        private String to;
        private String relation; // imports, calls, uses, depends_on, contains
    }
}
