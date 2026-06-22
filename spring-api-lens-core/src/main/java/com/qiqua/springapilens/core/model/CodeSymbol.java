package com.qiqua.springapilens.core.model;

public record CodeSymbol(
    String relativeFile,
    String symbolType,
    String className,
    String methodName,
    String signature,
    int lineStart,
    int lineEnd
) {
}
