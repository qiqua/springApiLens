package com.qiqua.springapilens.app.config;

import com.qiqua.springapilens.app.api.AiConfigResponse;
import com.qiqua.springapilens.app.api.AiConfigUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesDirectApiKeyWithoutReturningRawSecret() {
        AiConfigService service = new AiConfigService(tempDir.resolve("ai-config.json"));

        AiConfigResponse response = service.save(new AiConfigUpdateRequest(
            true,
            "bmapi",
            "https://api.example.test",
            "gpt-5.5",
            "",
            "sk-direct-test"
        ));

        assertThat(response.configured()).isTrue();
        assertThat(response.apiKeyConfigured()).isTrue();
        assertThat(response.apiKeyEnv()).isEmpty();
        assertThat(response.toString()).doesNotContain("sk-direct-test");
        assertThat(service.load().apiKey()).isEqualTo("sk-direct-test");
    }

    @Test
    void treatsEnvironmentVariableNamesAsApiKeyEnv() {
        AiConfigService service = new AiConfigService(tempDir.resolve("ai-config.json"));

        service.save(new AiConfigUpdateRequest(
            true,
            "local",
            "http://127.0.0.1:11434",
            "qwen",
            "",
            "LOCAL_AI_KEY"
        ));

        AiConfigResponse response = service.status();

        assertThat(response.configured()).isFalse();
        assertThat(response.apiKeyConfigured()).isFalse();
        assertThat(response.apiKeyEnv()).isEqualTo("LOCAL_AI_KEY");
    }
}
