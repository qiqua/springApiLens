package com.qiqua.springapilens.app.ai;

@FunctionalInterface
public interface AiClient {
    String complete(AiAnalysisRequest request);
}
