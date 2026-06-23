package com.qiqua.springapilens.app.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

public final class AiConfigLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiConfigLoader() {
    }

    public static AiConfig load(Path configPath, Function<String, String> envReader) {
        if (configPath == null || !Files.exists(configPath)) {
            return AiConfig.disabled();
        }
        try {
            RawConfig raw = OBJECT_MAPPER.readValue(configPath.toFile(), RawConfig.class);
            String apiKeyEnv = trim(raw.apiKeyEnv);
            String directApiKey = trim(raw.apiKey);
            ResolvedSecret secret = resolveSecret(directApiKey, apiKeyEnv, envReader);
            return new AiConfig(
                raw.enabled,
                trim(raw.provider),
                trim(raw.baseUrl),
                trim(raw.model),
                secret.apiKeyEnv(),
                secret.apiKey()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load AI config from " + configPath, exception);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResolvedSecret resolveSecret(
        String directApiKey,
        String apiKeyEnv,
        Function<String, String> envReader
    ) {
        if (!directApiKey.isBlank()) {
            return new ResolvedSecret("", directApiKey);
        }
        if (apiKeyEnv.isBlank()) {
            return new ResolvedSecret("", "");
        }
        if (looksLikeEnvironmentVariableName(apiKeyEnv)) {
            return new ResolvedSecret(apiKeyEnv, trim(envReader.apply(apiKeyEnv)));
        }
        return new ResolvedSecret("", apiKeyEnv);
    }

    private static boolean looksLikeEnvironmentVariableName(String value) {
        return value.matches("[A-Z_][A-Z0-9_]*")
            && value.equals(value.toUpperCase(Locale.ROOT));
    }

    private static final class RawConfig {
        public boolean enabled;
        public String provider;
        public String baseUrl;
        public String model;
        public String apiKeyEnv;
        public String apiKey;
    }

    private record ResolvedSecret(String apiKeyEnv, String apiKey) {
    }
}
