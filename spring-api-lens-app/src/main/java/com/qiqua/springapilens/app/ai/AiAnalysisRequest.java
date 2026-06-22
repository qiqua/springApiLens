package com.qiqua.springapilens.app.ai;

public record AiAnalysisRequest(
    AiConfig config,
    String prompt
) {
}
