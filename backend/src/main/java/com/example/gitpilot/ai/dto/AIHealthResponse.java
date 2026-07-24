package com.example.gitpilot.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AIHealthResponse {
    private Integer healthScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private String providerName;
    private String model;
    private String fallbackUsed;
    private LocalDateTime generatedTime;
}
