package com.example.gitpilot.commit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Deterministic, evidence-based technical impact of a commit, derived only from changed-file
 * paths/extensions (and never from AI or speculation). Not persisted.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnicalImpactDto {
    /** Affected areas, e.g. Backend, Frontend, Database, Configuration, Testing, CI/CD, Infrastructure, Documentation. */
    @Builder.Default
    private List<String> affectedAreas = new java.util.ArrayList<>();
    /** Technologies inferred from file types actually present in the change, e.g. Java, TypeScript, SQL. */
    @Builder.Default
    private List<String> technologies = new java.util.ArrayList<>();
    /** Likely architectural area, e.g. "Backend service/controller layer". "Not determined" when unknown. */
    private String architecturalArea;
    /** Concise deterministic summary line, e.g. "Modified 4 files across Backend, Frontend (+120/-35)." */
    private String summary;
}
