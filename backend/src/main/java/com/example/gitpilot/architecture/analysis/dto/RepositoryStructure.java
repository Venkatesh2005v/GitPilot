package com.example.gitpilot.architecture.analysis.dto;

import lombok.Builder;
import lombok.Data;
import java.util.*;

@Data
@Builder
public class RepositoryStructure {
    @Builder.Default private List<String> folders = new ArrayList<>();
    @Builder.Default private List<String> files = new ArrayList<>();
    @Builder.Default private Map<String, List<String>> filesByFolder = new HashMap<>();
    @Builder.Default private Set<String> extensions = new HashSet<>();
    @Builder.Default private List<String> manifestFiles = new ArrayList<>();
    // file path -> content (only for key files)
    @Builder.Default private Map<String, String> fileContents = new HashMap<>();
}
