package com.example.gitpilot.architecture.analysis.service;

import com.example.gitpilot.architecture.analysis.dto.RepositoryStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FrameworkDetectionService {

    public record DetectedFramework(String name, String language, String architecture) {}

    public DetectedFramework detect(RepositoryStructure structure) {
        Set<String> manifests = new HashSet<>();
        structure.getManifestFiles().forEach(m -> manifests.add(m.toLowerCase().contains("/") ? m.substring(m.lastIndexOf('/') + 1).toLowerCase() : m.toLowerCase()));
        Set<String> exts = structure.getExtensions();
        Set<String> folders = new HashSet<>();
        structure.getFolders().forEach(f -> folders.add(f.toLowerCase().contains("/") ? f.substring(f.lastIndexOf('/') + 1).toLowerCase() : f.toLowerCase()));

        // Spring Boot
        if (manifests.contains("pom.xml") || manifests.contains("build.gradle")) {
            if (exts.contains(".java") || exts.contains(".kt")) {
                return new DetectedFramework("Spring Boot", "Java", "Layered MVC");
            }
        }
        // Next.js
        if (manifests.stream().anyMatch(m -> m.startsWith("next.config"))) {
            return new DetectedFramework("Next.js", "TypeScript", "Full-Stack React");
        }
        // Angular
        if (manifests.contains("angular.json")) {
            return new DetectedFramework("Angular", "TypeScript", "Component-Based SPA");
        }
        // Flutter
        if (manifests.contains("pubspec.yaml")) {
            return new DetectedFramework("Flutter", "Dart", "Widget Tree");
        }
        // Django / FastAPI / Flask
        if (manifests.contains("requirements.txt") || manifests.contains("pyproject.toml")) {
            if (folders.contains("manage.py") || structure.getFiles().stream().anyMatch(f -> f.contains("manage.py")))
                return new DetectedFramework("Django", "Python", "MTV Architecture");
            return new DetectedFramework("Python", "Python", "Backend Service");
        }
        // Go
        if (manifests.contains("go.mod")) {
            return new DetectedFramework("Go", "Go", "Package-Based");
        }
        // Rust
        if (manifests.contains("cargo.toml")) {
            return new DetectedFramework("Rust", "Rust", "Module-Based");
        }
        // React / Vite / Node
        if (manifests.contains("package.json")) {
            if (manifests.stream().anyMatch(m -> m.startsWith("vite.config"))) {
                return new DetectedFramework("Vite + React", "TypeScript", "Component-Based SPA");
            }
            if (exts.contains(".tsx") || exts.contains(".jsx")) {
                return new DetectedFramework("React", "TypeScript", "Component-Based SPA");
            }
            if (folders.contains("routes") || folders.contains("controllers")) {
                return new DetectedFramework("Express/Node.js", "JavaScript", "Route-Based Backend");
            }
            return new DetectedFramework("Node.js", "JavaScript", "Module-Based");
        }
        // PHP
        if (manifests.contains("composer.json")) {
            return new DetectedFramework("PHP", "PHP", "MVC");
        }

        String lang = exts.contains(".java") ? "Java" : exts.contains(".py") ? "Python" :
                exts.contains(".ts") ? "TypeScript" : exts.contains(".go") ? "Go" : "Unknown";
        return new DetectedFramework("Generic", lang, "Unknown");
    }
}
