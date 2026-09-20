package com.example.gitpilot.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechStackDto {
    private List<String> detectedTechnologies;
    private List<String> detectedManifestFiles;
    private String primaryLanguage;
    private String category; // e.g. Full-Stack Java/React, Microservice, etc.

    /**
     * Phase 2: deterministic, evidence-backed technology fingerprint derived from actual file
     * contents. Additive/optional — existing consumers keep working if this is null.
     */
    private RepositoryFingerprintDto fingerprint;
}
