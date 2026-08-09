package com.example.gitpilot.architecture.codeanalysis.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AstParserEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedClassInfo {
        private String packageName;
        private String className;
        private String fullQualifiedName;
        private String type; // CLASS, INTERFACE, ENUM, ANNOTATION
        private String superClass;
        @Builder.Default
        private List<String> interfaces = new ArrayList<>();
        @Builder.Default
        private List<String> imports = new ArrayList<>();
        @Builder.Default
        private List<String> annotations = new ArrayList<>();
        @Builder.Default
        private List<ParsedMethodInfo> methods = new ArrayList<>();
        @Builder.Default
        private List<String> fields = new ArrayList<>();
        @Builder.Default
        private List<String> dependencies = new ArrayList<>();
        private String layerCategory; // CONTROLLER, SERVICE, REPOSITORY, ENTITY, DTO, CONFIG, COMPONENT
        private int loc;
        private int cyclomaticComplexity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedMethodInfo {
        private String methodName;
        private String returnType;
        private List<String> parameters;
        private List<String> annotations;
        private String httpEndpoint; // e.g. GET /api/users
        private int complexity;
    }

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([a-zA-Z0-9_.*]+);", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:@([a-zA-Z0-9_]+)\\s+)*(?:public|protected|private)?\\s*(?:abstract|final|static)?\\s*(class|interface|enum)\\s+([a-zA-Z0-9_]+)(?:<[^>]+>)?(?:\\s+extends\\s+([a-zA-Z0-9_.]+))?(?:\\s+implements\\s+([a-zA-Z0-9_,\\s.]+))?"
    );
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping|Override|Transactional)\\s*(?:\\([^)]*\\))?\\s*)*(?:public|protected|private|static|final|synchronized)*\\s+([a-zA-Z0-9_<>?,\\s\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)"
    );
    private static final Pattern MAPPING_VAL_PATTERN = Pattern.compile("(?:value|path)?\\s*=\\s*\"([^\"]+)\"");

    public ParsedClassInfo parseSourceCode(String fileName, String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return null;
        }

        String packageName = "";
        Matcher pkgMatcher = PACKAGE_PATTERN.matcher(sourceCode);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1);
        }

        List<String> imports = new ArrayList<>();
        Matcher impMatcher = IMPORT_PATTERN.matcher(sourceCode);
        while (impMatcher.find()) {
            imports.add(impMatcher.group(1));
        }

        String className = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String type = "CLASS";
        String superClass = null;
        List<String> interfaces = new ArrayList<>();
        List<String> annotations = extractTopAnnotations(sourceCode);

        Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
        if (classMatcher.find()) {
            type = classMatcher.group(1).toUpperCase();
            className = classMatcher.group(2);
            superClass = classMatcher.group(3);
            String implStr = classMatcher.group(4);
            if (implStr != null && !implStr.isBlank()) {
                for (String impl : implStr.split(",")) {
                    interfaces.add(impl.trim());
                }
            }
        }

        String fqn = packageName.isEmpty() ? className : packageName + "." + className;
        String layerCategory = determineLayerCategory(className, annotations, packageName);

        int loc = calculateLoc(sourceCode);
        int complexity = calculateCyclomaticComplexity(sourceCode);

        List<ParsedMethodInfo> methods = extractMethods(sourceCode);
        List<String> dependencies = extractFieldDependencies(sourceCode, imports);

        return ParsedClassInfo.builder()
                .packageName(packageName)
                .className(className)
                .fullQualifiedName(fqn)
                .type(type)
                .superClass(superClass)
                .interfaces(interfaces)
                .imports(imports)
                .annotations(annotations)
                .methods(methods)
                .dependencies(dependencies)
                .layerCategory(layerCategory)
                .loc(loc)
                .cyclomaticComplexity(complexity)
                .build();
    }

    private List<String> extractTopAnnotations(String source) {
        List<String> annotations = new ArrayList<>();
        Pattern annPattern = Pattern.compile("^@([a-zA-Z0-9_]+)", Pattern.MULTILINE);
        Matcher m = annPattern.matcher(source);
        while (m.find()) {
            annotations.add(m.group(1));
        }
        return annotations;
    }

    private String determineLayerCategory(String className, List<String> annotations, String packageName) {
        if (annotations.contains("RestController") || annotations.contains("Controller") || className.endsWith("Controller")) {
            return "CONTROLLER";
        }
        if (annotations.contains("Service") || className.endsWith("Service") || className.endsWith("ServiceImpl")) {
            return "SERVICE";
        }
        if (annotations.contains("Repository") || className.endsWith("Repository") || className.endsWith("Dao")) {
            return "REPOSITORY";
        }
        if (annotations.contains("Entity") || annotations.contains("Table") || packageName.contains(".entity")) {
            return "ENTITY";
        }
        if (className.endsWith("Dto") || className.endsWith("DTO") || className.endsWith("Response") || className.endsWith("Request")) {
            return "DTO";
        }
        if (annotations.contains("Configuration") || className.endsWith("Config")) {
            return "CONFIG";
        }
        return "COMPONENT";
    }

    private List<ParsedMethodInfo> extractMethods(String sourceCode) {
        List<ParsedMethodInfo> list = new ArrayList<>();
        Matcher m = METHOD_PATTERN.matcher(sourceCode);
        while (m.find()) {
            String returnType = m.group(2) != null ? m.group(2).trim() : "void";
            String methodName = m.group(3);
            String params = m.group(4);
            if ("if".equals(methodName) || "while".equals(methodName) || "for".equals(methodName) || "catch".equals(methodName) || "switch".equals(methodName)) {
                continue;
            }

            List<String> paramList = new ArrayList<>();
            if (params != null && !params.isBlank()) {
                for (String p : params.split(",")) {
                    paramList.add(p.trim());
                }
            }

            String httpEndpoint = null;
            if (sourceCode.contains("@GetMapping") || sourceCode.contains("@PostMapping") || sourceCode.contains("@PutMapping") || sourceCode.contains("@DeleteMapping")) {
                if (methodName.toLowerCase().contains("get") || sourceCode.contains("@GetMapping")) httpEndpoint = "GET /api/" + methodName;
                else if (methodName.toLowerCase().contains("post") || sourceCode.contains("@PostMapping")) httpEndpoint = "POST /api/" + methodName;
                else if (methodName.toLowerCase().contains("put") || sourceCode.contains("@PutMapping")) httpEndpoint = "PUT /api/" + methodName;
                else if (methodName.toLowerCase().contains("delete") || sourceCode.contains("@DeleteMapping")) httpEndpoint = "DELETE /api/" + methodName;
            }

            list.add(ParsedMethodInfo.builder()
                    .methodName(methodName)
                    .returnType(returnType)
                    .parameters(paramList)
                    .httpEndpoint(httpEndpoint)
                    .complexity(1 + (params != null && params.contains(",") ? 1 : 0))
                    .build());
        }
        return list;
    }

    private List<String> extractFieldDependencies(String sourceCode, List<String> imports) {
        Set<String> deps = new HashSet<>();
        Pattern fieldPattern = Pattern.compile("(?:private|protected|public)\\s+(?:final\\s+)?([a-zA-Z0-9_<>]+)\\s+([a-zA-Z0-9_]+);");
        Matcher m = fieldPattern.matcher(sourceCode);
        while (m.find()) {
            String fieldType = m.group(1);
            if (!isPrimitiveOrStandard(fieldType)) {
                deps.add(fieldType);
            }
        }
        return new ArrayList<>(deps);
    }

    private boolean isPrimitiveOrStandard(String type) {
        return List.of("String", "int", "long", "boolean", "double", "float", "Long", "Integer", "Boolean", "Double", "List", "Set", "Map", "Optional").contains(type);
    }

    private int calculateLoc(String source) {
        String[] lines = source.split("\r?\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                count++;
            }
        }
        return count;
    }

    private int calculateCyclomaticComplexity(String source) {
        int complexity = 1;
        String[] keywords = {"if\\b", "else\\b", "for\\b", "while\\b", "case\\b", "catch\\b", "&&", "\\|\\|", "\\?"};
        for (String kw : keywords) {
            Matcher m = Pattern.compile(kw).matcher(source);
            while (m.find()) {
                complexity++;
            }
        }
        return complexity;
    }
}
