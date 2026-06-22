package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.nio.file.Path;
import java.util.List;

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
        List<ApiEndpoint> endpoints = withAuthorContributions(repoRoot, new SpringEndpointExtractor().extract(repoRoot, files));
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> callEdges = new CallEdgeExtractor().extract(repoRoot, files, symbols);
        List<SqlFragment> sqlFragments = new MyBatisSqlExtractor().extract(repoRoot, files);
        return new ScanResult(repositoryInfo, endpoints, symbols, callEdges, sqlFragments);
    }

    private List<ApiEndpoint> withAuthorContributions(Path repoRoot, List<ApiEndpoint> endpoints) {
        return endpoints.stream()
            .map(endpoint -> endpointWithAuthors(repoRoot, endpoint))
            .toList();
    }

    private ApiEndpoint endpointWithAuthors(Path repoRoot, ApiEndpoint endpoint) {
        List<AuthorContribution> authors;
        try {
            authors = gitBlameAnalyzer.analyze(
                repoRoot,
                repoRoot.resolve(endpoint.relativeFile()),
                endpoint.lineStart(),
                endpoint.lineEnd()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            authors = List.of();
        }
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
}
