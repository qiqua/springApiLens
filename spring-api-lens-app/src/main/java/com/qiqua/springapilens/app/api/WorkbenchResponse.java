package com.qiqua.springapilens.app.api;

import java.util.List;

public record WorkbenchResponse(
    RepositoryView repository,
    SummaryView summary,
    List<EndpointListItem> endpoints,
    FilterView filters
) {
    public record RepositoryView(
        String repoName,
        String rootPath,
        String branchName,
        String headCommit,
        boolean hasUncommittedChanges
    ) {
    }

    public record SummaryView(
        int endpointCount,
        int callEdgeCount,
        int sqlFragmentCount,
        int tableCount
    ) {
    }

    public record EndpointListItem(
        String key,
        String httpMethod,
        String path,
        String className,
        String methodName,
        String requestBodyType,
        String responseType,
        String relativeFile,
        int lineStart,
        int lineEnd,
        List<String> tables,
        int callCount,
        List<String> authors
    ) {
    }

    public record FilterView(
        List<String> httpMethods,
        List<String> tables,
        List<String> authors
    ) {
    }
}
