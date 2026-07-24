package com.example.gitpilot.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AISummaryResponse {
    private String summary;
    private String providerName;
    private String model;
    private String fallbackUsed;
    private LocalDateTime generatedTime;
}
