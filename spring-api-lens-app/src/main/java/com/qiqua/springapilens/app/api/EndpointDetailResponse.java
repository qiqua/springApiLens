package com.qiqua.springapilens.app.api;

import java.util.List;

public record EndpointDetailResponse(
    EndpointView endpoint,
    BusinessProfileView profile,
    List<CallEdgeView> callEdges,
    List<SqlFragmentView> sqlFragments,
    List<String> tables,
    List<AuthorView> authors
) {
    public record EndpointView(
        String key,
        String httpMethod,
        String path,
        String className,
        String methodName,
        String requestParamsJson,
        String requestBodyType,
        String responseType,
        String relativeFile,
        int lineStart,
        int lineEnd
    ) {
    }

    public record CallEdgeView(
        String fromSignature,
        String toSignature,
        double confidence,
        String evidence
    ) {
    }

    public record SqlFragmentView(
        String relativeFile,
        String mapperNamespace,
        String mapperMethod,
        String sqlText,
        List<String> tables,
        String operationType
    ) {
    }

    public record AuthorView(
        String name,
        String email,
        double ratio,
        int lineCount
    ) {
    }

    public record BusinessProfileView(
        String purpose,
        String callGuide,
        List<String> businessFlow,
        List<String> dataTables,
        List<String> authorSummary,
        List<String> risks,
        List<String> testSuggestions
    ) {
    }
}
