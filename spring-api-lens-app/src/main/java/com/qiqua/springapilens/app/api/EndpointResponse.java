package com.qiqua.springapilens.app.api;

public record EndpointResponse(
    String httpMethod,
    String path,
    String className,
    String methodName,
    String requestBodyType,
    String responseType
) {
}
