package com.qiqua.springapilens.app.api;

public record AiConfigUpdateRequest(
    boolean enabled,
    String provider,
    String baseUrl,
    String model,
    String apiKeyEnv,
    String apiKey
) {
}
