package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringEndpointExtractor {
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(?:public\\s+)?(?:class|interface)\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?:public|protected|private)?\\s*(?!if\\b|for\\b|while\\b|switch\\b|catch\\b)([\\w<>?,.\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{?"
    );
    private static final Pattern STRING_VALUE_PATTERN = Pattern.compile("\"([^\"]*)\"");

    public List<ApiEndpoint> extract(Path repoRoot, List<Path> javaFiles) {
        Path root = repoRoot.toAbsolutePath().normalize();
        List<ApiEndpoint> endpoints = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            endpoints.addAll(extractFromFile(root, javaFile.toAbsolutePath().normalize()));
        }
        return endpoints;
    }

    private List<ApiEndpoint> extractFromFile(Path repoRoot, Path javaFile) {
        try {
            List<String> lines = Files.readAllLines(javaFile);
            if (!containsControllerAnnotation(lines)) {
                return List.of();
            }
            String className = findClassName(lines).orElse("");
            String classPath = "";
            List<String> pendingAnnotations = new ArrayList<>();
            List<ApiEndpoint> endpoints = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("@")) {
                    pendingAnnotations.add(line);
                    continue;
                }
                if (line.contains(" class ") || line.startsWith("class ") || line.contains(" interface ") || line.startsWith("interface ")) {
                    classPath = findMappingPath(pendingAnnotations).orElse("");
                    pendingAnnotations.clear();
                    continue;
                }
                Mapping mapping = findMethodMapping(pendingAnnotations).orElse(null);
                if (mapping != null) {
                    MethodSignature signature = parseMethod(line).orElse(null);
                    if (signature != null) {
                        endpoints.add(new ApiEndpoint(
                            repoRoot.relativize(javaFile).toString().replace('\\', '/'),
                            className,
                            signature.methodName(),
                            mapping.httpMethod(),
                            joinPaths(classPath, mapping.path()),
                            "[]",
                            requestBodyType(signature.parameters()),
                            signature.returnType(),
                            i + 1,
                            findMethodEndLine(lines, i)
                        ));
                    }
                    pendingAnnotations.clear();
                    continue;
                }
                if (!line.isBlank() && !line.startsWith("//")) {
                    pendingAnnotations.clear();
                }
            }
            return endpoints;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
        }
    }

    private boolean containsControllerAnnotation(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains("@RestController") || line.contains("@Controller"));
    }

    private Optional<String> findClassName(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = CLASS_PATTERN.matcher(line);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private Optional<Mapping> findMethodMapping(List<String> annotations) {
        for (String annotation : annotations) {
            if (annotation.startsWith("@GetMapping")) {
                return Optional.of(new Mapping("GET", firstStringValue(annotation).orElse("")));
            }
            if (annotation.startsWith("@PostMapping")) {
                return Optional.of(new Mapping("POST", firstStringValue(annotation).orElse("")));
            }
            if (annotation.startsWith("@PutMapping")) {
                return Optional.of(new Mapping("PUT", firstStringValue(annotation).orElse("")));
            }
            if (annotation.startsWith("@DeleteMapping")) {
                return Optional.of(new Mapping("DELETE", firstStringValue(annotation).orElse("")));
            }
            if (annotation.startsWith("@RequestMapping")) {
                return Optional.of(new Mapping("REQUEST", firstStringValue(annotation).orElse("")));
            }
        }
        return Optional.empty();
    }

    private Optional<String> findMappingPath(List<String> annotations) {
        return annotations.stream()
            .filter(annotation -> annotation.startsWith("@RequestMapping"))
            .findFirst()
            .flatMap(this::firstStringValue);
    }

    private Optional<String> firstStringValue(String annotation) {
        Matcher matcher = STRING_VALUE_PATTERN.matcher(annotation);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<MethodSignature> parseMethod(String line) {
        Matcher matcher = METHOD_PATTERN.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new MethodSignature(
            normalizeType(matcher.group(1)),
            matcher.group(2),
            matcher.group(3)
        ));
    }

    private String requestBodyType(String parameters) {
        if (!parameters.contains("@RequestBody")) {
            return "";
        }
        String cleaned = parameters.replace("@RequestBody", "").trim();
        String[] parts = cleaned.split("\\s+");
        return parts.length == 0 ? "" : parts[0];
    }

    private String normalizeType(String type) {
        return type.replaceAll("\\s+", " ").trim();
    }

    private String joinPaths(String classPath, String methodPath) {
        String joined = ("/" + classPath + "/" + methodPath).replaceAll("/+", "/");
        if (joined.length() > 1 && joined.endsWith("/")) {
            return joined.substring(0, joined.length() - 1);
        }
        return joined;
    }

    private int findMethodEndLine(List<String> lines, int startIndex) {
        int depth = 0;
        boolean sawOpeningBrace = false;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            for (int j = 0; j < line.length(); j++) {
                char value = line.charAt(j);
                if (value == '{') {
                    depth++;
                    sawOpeningBrace = true;
                } else if (value == '}') {
                    depth--;
                    if (sawOpeningBrace && depth <= 0) {
                        return i + 1;
                    }
                }
            }
        }
        return startIndex + 1;
    }

    private record Mapping(String httpMethod, String path) {
    }

    private record MethodSignature(String returnType, String methodName, String parameters) {
    }
}
