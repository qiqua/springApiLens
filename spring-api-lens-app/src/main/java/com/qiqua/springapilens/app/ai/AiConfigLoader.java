package com.qiqua.springapilens.app.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            return new AiConfig(
                raw.enabled,
                trim(raw.provider),
                trim(raw.baseUrl),
                trim(raw.model),
                apiKeyEnv,
                apiKeyEnv.isBlank() ? "" : trim(envReader.apply(apiKeyEnv))
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load AI config from " + configPath, exception);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class RawConfig {
        public boolean enabled;
        public String provider;
        public String baseUrl;
        public String model;
        public String apiKeyEnv;
    }
}
