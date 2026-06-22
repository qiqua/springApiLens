package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.CodeSymbol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSymbolExtractor {
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(?:class|interface)\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "\\b(?:public\\s+|protected\\s+|private\\s+)?(?:static\\s+)?(?:final\\s+)?(?!if\\b|for\\b|while\\b|switch\\b|catch\\b|return\\b|new\\b)([\\w<>?,.\\[\\]]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[\\w.,\\s]+)?\\s*(?:\\{|;)"
    );

    public List<CodeSymbol> extract(Path repoRoot, List<Path> javaFiles) {
        Path root = repoRoot.toAbsolutePath().normalize();
        List<CodeSymbol> symbols = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            symbols.addAll(extractFile(root, javaFile.toAbsolutePath().normalize()));
        }
        return symbols;
    }

    private List<CodeSymbol> extractFile(Path repoRoot, Path javaFile) {
        try {
            List<String> lines = Files.readAllLines(javaFile);
            String className = findTypeName(lines).orElse("");
            List<CodeSymbol> symbols = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                Matcher matcher = METHOD_PATTERN.matcher(lines.get(i));
                while (matcher.find()) {
                    String methodName = matcher.group(2);
                    if (methodName.equals(className)) {
                        continue;
                    }
                    String signature = className + "." + methodName + "()";
                    symbols.add(new CodeSymbol(
                        repoRoot.relativize(javaFile).toString().replace('\\', '/'),
                        "METHOD",
                        className,
                        methodName,
                        signature,
                        i + 1,
                        findMethodEndLine(lines, i)
                    ));
                }
            }
            return symbols;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
        }
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
