package com.qiqua.springapilens.app.ai;

public record AiSummary(
    boolean enabled,
    boolean configured,
    String provider,
    String model,
    String content,
    String message
) {
    public static AiSummary disabled(AiConfig config) {
        return new AiSummary(
            config.enabled(),
            false,
            config.provider(),
            config.model(),
            "",
            config.enabled()
                ? "AI is enabled but baseUrl, model, or API key is missing."
                : "AI is disabled. Configure .spring-api-lens/ai-config.json to enable summaries."
        );
    }
}
