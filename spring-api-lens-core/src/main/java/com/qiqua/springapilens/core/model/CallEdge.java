package com.qiqua.springapilens.core.model;

public record CallEdge(
    String fromSignature,
    String toSignature,
    double confidence,
    String evidence
) {
}
