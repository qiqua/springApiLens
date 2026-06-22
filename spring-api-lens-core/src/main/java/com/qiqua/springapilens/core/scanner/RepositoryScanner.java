package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.nio.file.Path;
import java.util.List;

public class RepositoryScanner {
    public ScanResult scan(Path repoRoot) {
        RepositoryInfo repositoryInfo = new RepositoryValidator().validate(repoRoot);
        List<Path> files = new SourceFileDiscoverer().discover(repoRoot);
        List<ApiEndpoint> endpoints = new SpringEndpointExtractor().extract(repoRoot, files);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> callEdges = new CallEdgeExtractor().extract(repoRoot, files, symbols);
        List<SqlFragment> sqlFragments = new MyBatisSqlExtractor().extract(repoRoot, files);
        return new ScanResult(repositoryInfo, endpoints, symbols, callEdges, sqlFragments);
    }
}
