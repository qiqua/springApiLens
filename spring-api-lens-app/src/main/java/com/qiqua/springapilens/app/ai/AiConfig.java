package com.qiqua.springapilens.app.ai;

public record AiConfig(
    boolean enabled,
    String provider,
    String baseUrl,
    String model,
    String apiKeyEnv,
    String apiKey
) {
    public boolean configured() {
        return enabled
            && !isBlank(baseUrl)
            && !isBlank(model)
            && !isBlank(apiKey);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static AiConfig disabled() {
        return new AiConfig(false, "", "", "", "", "");
    }
}
