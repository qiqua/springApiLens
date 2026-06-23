package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepositoryScanner {
    private final GitBlameAnalyzer gitBlameAnalyzer;

    public RepositoryScanner() {
        this(new GitBlameAnalyzer());
    }

    RepositoryScanner(GitBlameAnalyzer gitBlameAnalyzer) {
        this.gitBlameAnalyzer = gitBlameAnalyzer;
    }

    public ScanResult scan(Path repoRoot) {
        RepositoryInfo repositoryInfo = new RepositoryValidator().validate(repoRoot);
        List<Path> files = new SourceFileDiscoverer().discover(repoRoot);
        List<ApiEndpoint> rawEndpoints = new SpringEndpointExtractor().extract(repoRoot, files);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> callEdges = new CallEdgeExtractor().extract(repoRoot, files, symbols);
        List<SqlFragment> sqlFragments = new MyBatisSqlExtractor().extract(repoRoot, files);
        List<ApiEndpoint> endpoints = withAuthorContributions(repoRoot, rawEndpoints, symbols, callEdges);
        return new ScanResult(repositoryInfo, endpoints, symbols, callEdges, sqlFragments);
    }

    private List<ApiEndpoint> withAuthorContributions(
        Path repoRoot,
        List<ApiEndpoint> endpoints,
        List<CodeSymbol> symbols,
        List<CallEdge> callEdges
    ) {
        Map<String, CodeSymbol> symbolsBySignature = symbolsBySignature(symbols);
        Map<BlameRange, List<AuthorContribution>> blameCache = new HashMap<>();
        Map<String, List<AuthorContribution>> fileHistoryCache = new HashMap<>();
        return endpoints.stream()
            .map(endpoint -> endpointWithAuthors(repoRoot, endpoint, symbolsBySignature, callEdges, blameCache, fileHistoryCache))
            .toList();
    }

    private Map<String, CodeSymbol> symbolsBySignature(List<CodeSymbol> symbols) {
        Map<String, CodeSymbol> symbolsBySignature = new HashMap<>();
        for (CodeSymbol symbol : symbols) {
            symbolsBySignature.put(symbol.signature(), symbol);
            symbolsBySignature.put(normalizedSignature(symbol.signature()), symbol);
        }
        return symbolsBySignature;
    }

    private ApiEndpoint endpointWithAuthors(
        Path repoRoot,
        ApiEndpoint endpoint,
        Map<String, CodeSymbol> symbolsBySignature,
        List<CallEdge> callEdges,
        Map<BlameRange, List<AuthorContribution>> blameCache,
        Map<String, List<AuthorContribution>> fileHistoryCache
    ) {
        List<AuthorContribution> authors = aggregateAuthorContributions(
            repoRoot,
            blameRanges(endpoint, symbolsBySignature, callEdges),
            blameCache,
            fileHistoryCache
        );
        return new ApiEndpoint(
            endpoint.relativeFile(),
            endpoint.className(),
            endpoint.methodName(),
            endpoint.httpMethod(),
            endpoint.path(),
            endpoint.requestParamsJson(),
            endpoint.requestBodyType(),
            endpoint.responseType(),
            endpoint.lineStart(),
            endpoint.lineEnd(),
            authors
        );
    }

    private List<BlameRange> blameRanges(
        ApiEndpoint endpoint,
        Map<String, CodeSymbol> symbolsBySignature,
        List<CallEdge> callEdges
    ) {
        Set<BlameRange> ranges = new LinkedHashSet<>();
        ranges.add(new BlameRange(endpoint.relativeFile(), endpoint.lineStart(), endpoint.lineEnd()));
        for (CallEdge edge : relatedCallEdges(callEdges, endpoint)) {
            addSymbolRange(ranges, symbolsBySignature, edge.fromSignature());
            addSymbolRange(ranges, symbolsBySignature, edge.toSignature());
        }
        return List.copyOf(ranges);
    }

    private void addSymbolRange(Set<BlameRange> ranges, Map<String, CodeSymbol> symbolsBySignature, String signature) {
        CodeSymbol symbol = symbolsBySignature.get(signature);
        if (symbol == null) {
            symbol = symbolsBySignature.get(normalizedSignature(signature));
        }
        if (symbol != null) {
            ranges.add(new BlameRange(symbol.relativeFile(), symbol.lineStart(), symbol.lineEnd()));
        }
    }

    private List<CallEdge> relatedCallEdges(List<CallEdge> callEdges, ApiEndpoint endpoint) {
        String controllerMethod = endpoint.className() + "." + endpoint.methodName();
        Set<String> frontier = new LinkedHashSet<>();
        Set<CallEdge> related = new LinkedHashSet<>();
        frontier.add(controllerMethod);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (CallEdge edge : callEdges) {
                if (!related.contains(edge) && matchesAny(edge.fromSignature(), frontier)) {
                    related.add(edge);
                    frontier.add(normalizedSignature(edge.toSignature()));
                    changed = true;
                }
            }
        }
        return List.copyOf(related);
    }

    private boolean matchesAny(String signature, Set<String> methodNames) {
        return methodNames.contains(normalizedSignature(signature));
    }

    private String normalizedSignature(String signature) {
        int parameterStart = signature.indexOf('(');
        if (parameterStart > 0) {
            return signature.substring(0, parameterStart);
        }
        return signature;
    }

    private List<AuthorContribution> aggregateAuthorContributions(
        Path repoRoot,
        List<BlameRange> ranges,
        Map<BlameRange, List<AuthorContribution>> blameCache,
        Map<String, List<AuthorContribution>> fileHistoryCache
    ) {
        Map<AuthorKey, MutableContribution> totals = new LinkedHashMap<>();
        Set<String> historyFiles = new LinkedHashSet<>();
        for (BlameRange range : ranges) {
            List<AuthorContribution> rangeAuthors = blameCache.computeIfAbsent(range, ignored -> analyzeRange(repoRoot, range));
            addContributions(totals, rangeAuthors);
            historyFiles.add(range.relativeFile());
        }
        for (String relativeFile : historyFiles) {
            List<AuthorContribution> fileAuthors = fileHistoryCache.computeIfAbsent(
                relativeFile,
                ignored -> analyzeFileHistory(repoRoot, relativeFile)
            );
            addContributions(totals, fileAuthors);
        }

        int totalLines = totals.values().stream().mapToInt(value -> value.lineCount).sum();
        return totals.values().stream()
            .map(value -> new AuthorContribution(
                value.name,
                value.email,
                totalLines == 0 ? 0.0 : (double) value.lineCount / totalLines,
                value.lineCount
            ))
            .toList();
    }

    private void addContributions(
        Map<AuthorKey, MutableContribution> totals,
        List<AuthorContribution> contributions
    ) {
        for (AuthorContribution author : contributions) {
            AuthorKey key = new AuthorKey(author.name(), author.email());
            totals.computeIfAbsent(key, ignored -> new MutableContribution(author.name(), author.email()))
                .lineCount += author.lineCount();
        }
    }

    private List<AuthorContribution> analyzeRange(Path repoRoot, BlameRange range) {
        try {
            return gitBlameAnalyzer.analyze(
                repoRoot,
                repoRoot.resolve(range.relativeFile()),
                range.lineStart(),
                range.lineEnd()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return List.of();
        }
    }

    private List<AuthorContribution> analyzeFileHistory(Path repoRoot, String relativeFile) {
        try {
            String output = run(repoRoot, "git", "log", "--follow", "--format=%an%x00%ae", "--", relativeFile);
            Map<AuthorKey, MutableContribution> counts = new LinkedHashMap<>();
            for (String line : output.split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\x00", 2);
                String name = parts.length > 0 ? parts[0] : "";
                String email = parts.length > 1 ? parts[1] : "";
                AuthorKey key = new AuthorKey(name, email);
                counts.computeIfAbsent(key, ignored -> new MutableContribution(name, email)).lineCount++;
            }
            return counts.values().stream()
                .map(value -> new AuthorContribution(value.name, value.email, 0.0, value.lineCount))
                .toList();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return List.of();
        }
    }

    private String run(Path repoRoot, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalArgumentException(String.join(" ", command) + " failed: " + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + String.join(" ", command), e);
        }
    }

    private record BlameRange(String relativeFile, int lineStart, int lineEnd) {
    }

    private record AuthorKey(String name, String email) {
    }

    private static final class MutableContribution {
        private final String name;
        private final String email;
        private int lineCount;

        private MutableContribution(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
