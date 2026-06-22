package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallEdgeExtractor {
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(?:class|interface)\\s+(\\w+)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\b(?:private|protected|public)?\\s*(?:final\\s+)?(\\w+)\\s+(\\w+)\\s*;");
    private static final Pattern CONSTRUCTOR_PARAMETER_PATTERN = Pattern.compile("(\\w+)\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:public|protected|private)?\\s*(?!if\\b|for\\b|while\\b|switch\\b|catch\\b|return\\b|new\\b)([\\w<>?,.\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|;).*"
    );
    private static final Pattern CALL_PATTERN = Pattern.compile("\\b(\\w+)\\.(\\w+)\\s*\\(");

    public List<CallEdge> extract(Path repoRoot, List<Path> javaFiles, List<CodeSymbol> symbols) {
        Map<String, CodeSymbol> byClassAndMethod = new HashMap<>();
        for (CodeSymbol symbol : symbols) {
            byClassAndMethod.put(symbol.className() + "." + symbol.methodName(), symbol);
        }

        List<CallEdge> edges = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            edges.addAll(extractFile(javaFile, byClassAndMethod));
        }
        return edges;
    }

    private List<CallEdge> extractFile(Path javaFile, Map<String, CodeSymbol> byClassAndMethod) {
        try {
            List<String> lines = Files.readAllLines(javaFile);
            String className = findTypeName(lines).orElse("");
            Map<String, String> variableTypes = variableTypes(lines, className);
            List<CallEdge> edges = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                Matcher methodMatcher = METHOD_PATTERN.matcher(lines.get(i));
                if (!methodMatcher.matches()) {
                    continue;
                }
                String methodName = methodMatcher.group(2);
                if (methodName.equals(className)) {
                    continue;
                }
                String fromSignature = className + "." + methodName + "()";
                int endLine = findMethodEndLine(lines, i);
                for (int lineIndex = i; lineIndex < endLine && lineIndex < lines.size(); lineIndex++) {
                    Matcher callMatcher = CALL_PATTERN.matcher(lines.get(lineIndex));
                    while (callMatcher.find()) {
                        String variable = callMatcher.group(1);
                        String targetMethod = callMatcher.group(2);
                        String targetClass = variableTypes.get(variable);
                        if (targetClass == null) {
                            continue;
                        }
                        CodeSymbol target = byClassAndMethod.get(targetClass + "." + targetMethod);
                        if (target != null) {
                            edges.add(new CallEdge(
                                fromSignature,
                                target.signature(),
                                0.95,
                                variable + "." + targetMethod
                            ));
                        } else if (shouldKeepUnresolvedCall(targetClass)) {
                            edges.add(new CallEdge(
                                fromSignature,
                                targetClass + "." + targetMethod + "()",
                                0.65,
                                variable + "." + targetMethod
                            ));
                        }
                    }
                }
            }
            return edges;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
        }
    }

    private boolean shouldKeepUnresolvedCall(String targetClass) {
        return targetClass.endsWith("Mapper")
            || targetClass.endsWith("Repository")
            || targetClass.endsWith("Repo")
            || targetClass.endsWith("Dao");
    }

    private Map<String, String> variableTypes(List<String> lines, String className) {
        Map<String, String> variableTypes = new HashMap<>();
        for (String line : lines) {
            Matcher fieldMatcher = FIELD_PATTERN.matcher(line.trim());
            if (fieldMatcher.find()) {
                variableTypes.put(fieldMatcher.group(2), fieldMatcher.group(1));
            }
            if (line.contains(className + "(")) {
                int start = line.indexOf('(');
                int end = line.indexOf(')', start);
                if (start >= 0 && end > start) {
                    String parameters = line.substring(start + 1, end);
                    for (String parameter : parameters.split(",")) {
                        Matcher parameterMatcher = CONSTRUCTOR_PARAMETER_PATTERN.matcher(parameter.trim());
                        if (parameterMatcher.matches()) {
                            variableTypes.put(parameterMatcher.group(2), parameterMatcher.group(1));
                        }
                    }
                }
            }
        }
        return variableTypes;
    }

    private Optional<String> findTypeName(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = TYPE_PATTERN.matcher(line);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
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
            if (!sawOpeningBrace && line.contains(";")) {
                return i + 1;
            }
        }
        return startIndex + 1;
    }
}
