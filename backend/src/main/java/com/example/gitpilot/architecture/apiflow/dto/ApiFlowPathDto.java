package com.example.gitpilot.architecture.apiflow.dto;

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
public class ApiFlowPathDto {
    private String endpoint;
    private String httpMethod;
    private String controllerClass;
    private String controllerMethod;
    private String serviceClass;
    private String serviceMethod;
    private String repositoryClass;
    private String databaseTable;
    @Builder.Default
    private List<ApiFlowStepDto> flowSteps = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiFlowStepDto {
        private int stepOrder;
        private String layer; // CLIENT, CONTROLLER, SERVICE, REPOSITORY, DATABASE
        private String componentName;
        private String action;
        private String details;
    }
}
