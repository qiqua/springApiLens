package com.qiqua.springapilens.app.api;

public record AiConfigResponse(
    boolean enabled,
    boolean configured,
    String provider,
    String baseUrl,
    String model,
    String apiKeyEnv,
    String message
) {
}
