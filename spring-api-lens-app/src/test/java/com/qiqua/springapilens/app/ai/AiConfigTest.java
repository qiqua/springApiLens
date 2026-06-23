package com.qiqua.springapilens.app.ai;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigTest {
    @Test
    void loadsOpenAiCompatibleConfigFromLocalJson() throws Exception {
        Path configPath = Files.createTempFile("spring-api-lens-ai", ".json");
        Files.writeString(configPath, """
            {
              "enabled": true,
              "provider": "deepseek",
              "baseUrl": "https://api.deepseek.com",
              "model": "deepseek-chat",
              "apiKeyEnv": "SPRING_API_LENS_TEST_KEY"
            }
            """);

        AiConfig config = AiConfigLoader.load(configPath, key -> "test-secret");

        assertThat(config.enabled()).isTrue();
        assertThat(config.provider()).isEqualTo("deepseek");
        assertThat(config.baseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(config.model()).isEqualTo("deepseek-chat");
        assertThat(config.apiKey()).isEqualTo("test-secret");
    }

    @Test
    void loadsDirectApiKeyFromLocalJson() throws Exception {
        Path configPath = Files.createTempFile("spring-api-lens-ai-direct", ".json");
        Files.writeString(configPath, """
            {
              "enabled": true,
              "provider": "bmapi",
              "baseUrl": "https://api.example.test",
              "model": "gpt-5.5",
              "apiKey": "sk-direct-test"
            }
            """);

        AiConfig config = AiConfigLoader.load(configPath, key -> null);

        assertThat(config.configured()).isTrue();
        assertThat(config.apiKeyEnv()).isEmpty();
        assertThat(config.apiKey()).isEqualTo("sk-direct-test");
    }

    @Test
    void treatsLegacyApiKeyEnvRawSecretAsDirectApiKey() throws Exception {
        Path configPath = Files.createTempFile("spring-api-lens-ai-legacy", ".json");
        Files.writeString(configPath, """
            {
              "enabled": true,
              "provider": "bmapi",
              "baseUrl": "https://api.example.test",
              "model": "gpt-5.5",
              "apiKeyEnv": "sk-legacy-direct-test"
            }
            """);

        AiConfig config = AiConfigLoader.load(configPath, key -> "");

        assertThat(config.configured()).isTrue();
        assertThat(config.apiKeyEnv()).isEmpty();
        assertThat(config.apiKey()).isEqualTo("sk-legacy-direct-test");
    }

    @Test
    void returnsDisabledConfigWhenFileIsMissing() {
        AiConfig config = AiConfigLoader.load(Path.of("missing-ai-config.json"), key -> null);

        assertThat(config.enabled()).isFalse();
        assertThat(config.configured()).isFalse();
    }
}
