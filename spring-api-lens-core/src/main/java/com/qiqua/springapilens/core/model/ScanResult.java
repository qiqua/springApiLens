package com.qiqua.springapilens.core.model;

import java.util.List;

public record ScanResult(
    RepositoryInfo repositoryInfo,
    List<ApiEndpoint> endpoints,
    List<CodeSymbol> symbols,
    List<CallEdge> callEdges,
    List<SqlFragment> sqlFragments
) {
}
