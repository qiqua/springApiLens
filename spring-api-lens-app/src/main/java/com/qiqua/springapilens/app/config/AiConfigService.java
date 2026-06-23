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
import java.util.Locale;

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
            !config.apiKey().isBlank(),
            config.configured() ? "" : missingConfigMessage(config)
        );
    }

    public AiConfigResponse save(AiConfigUpdateRequest request) {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            PersistedAiConfig existing = readExistingConfig();
            SecretConfig secretConfig = secretConfig(request, existing);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), new PersistedAiConfig(
                request.enabled(),
                trim(request.provider()),
                trim(request.baseUrl()),
                trim(request.model()),
                secretConfig.apiKeyEnv(),
                secretConfig.apiKey()
            ));
            return status();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save AI config to " + configPath, exception);
        }
    }

    private PersistedAiConfig readExistingConfig() throws IOException {
        if (!Files.exists(configPath)) {
            return new PersistedAiConfig(false, "", "", "", "", "");
        }
        return OBJECT_MAPPER.readValue(configPath.toFile(), PersistedAiConfig.class);
    }

    private SecretConfig secretConfig(AiConfigUpdateRequest request, PersistedAiConfig existing) {
        String submittedSecret = trim(request.apiKey());
        if (submittedSecret.isBlank()) {
            submittedSecret = trim(request.apiKeyEnv());
        }
        if (submittedSecret.isBlank()) {
            return new SecretConfig(trim(existing.apiKeyEnv()), trim(existing.apiKey()));
        }
        if (looksLikeEnvironmentVariableName(submittedSecret)) {
            return new SecretConfig(submittedSecret, "");
        }
        return new SecretConfig("", submittedSecret);
    }

    private boolean looksLikeEnvironmentVariableName(String value) {
        return value.matches("[A-Z_][A-Z0-9_]*")
            && value.equals(value.toUpperCase(Locale.ROOT));
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
        String apiKeyEnv,
        String apiKey
    ) {
    }

    private record SecretConfig(
        String apiKeyEnv,
        String apiKey
    ) {
    }
}
