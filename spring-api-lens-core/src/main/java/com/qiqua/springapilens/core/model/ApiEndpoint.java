package com.qiqua.springapilens.core.model;

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
    int lineEnd
) {
}
