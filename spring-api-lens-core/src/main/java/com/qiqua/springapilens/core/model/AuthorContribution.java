package com.qiqua.springapilens.core.model;

public record AuthorContribution(
    String name,
    String email,
    double ratio,
    int lineCount
) {
}
