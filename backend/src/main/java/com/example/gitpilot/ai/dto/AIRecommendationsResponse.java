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
public class AIRecommendationsResponse {
    private List<String> recommendations;
    private String providerName;
    private String model;
    private String fallbackUsed;
    private LocalDateTime generatedTime;
}
