package com.qiqua.springapilens.core.model;

import java.util.List;

public record ApiEndpoint(
    String relativeFile,
    String className,
    String methodName,
    String httpMethod,
    String path,
    String requestParamsJson,
    String requestBodyType,
    String responseType,
    int lineStart,
    int lineEnd,
    List<AuthorContribution> authors
) {
    public ApiEndpoint(
        String relativeFile,
        String className,
        String methodName,
        String httpMethod,
        String path,
        String requestParamsJson,
        String requestBodyType,
        String responseType,
        int lineStart,
        int lineEnd
    ) {
        this(
            relativeFile,
            className,
            methodName,
            httpMethod,
            path,
            requestParamsJson,
            requestBodyType,
            responseType,
            lineStart,
            lineEnd,
            List.of()
        );
    }

    public ApiEndpoint {
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
