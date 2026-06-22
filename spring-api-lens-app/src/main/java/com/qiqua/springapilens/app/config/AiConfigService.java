package com.qiqua.springapilens.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiqua.springapilens.app.ai.AiConfig;
import com.qiqua.springapilens.app.ai.AiConfigLoader;
import com.qiqua.springapilens.app.api.AiConfigResponse;
import com.qiqua.springapilens.app.api.AiConfigUpdateRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AiConfigService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path configPath;

    public AiConfigService(Path configPath) {
        this.configPath = configPath;
    }

    public AiConfig load() {
        return AiConfigLoader.load(configPath, System::getenv);
    }

    public AiConfigResponse status() {
        AiConfig config = load();
        return new AiConfigResponse(
            config.enabled(),
            config.configured(),
            config.provider(),
            config.baseUrl(),
            config.model(),
            config.apiKeyEnv(),
            config.configured() ? "" : missingConfigMessage(config)
        );
    }

    public AiConfigResponse save(AiConfigUpdateRequest request) {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), new PersistedAiConfig(
                request.enabled(),
                trim(request.provider()),
                trim(request.baseUrl()),
                trim(request.model()),
                trim(request.apiKeyEnv())
            ));
            return status();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save AI config to " + configPath, exception);
        }
    }

    private String missingConfigMessage(AiConfig config) {
        if (!config.enabled()) {
            return "AI is disabled.";
        }
        return "AI is enabled but baseUrl, model, or API key is missing.";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private record PersistedAiConfig(
        boolean enabled,
        String provider,
        String baseUrl,
        String model,
        String apiKeyEnv
    ) {
    }
}
