package com.qiqua.springapilens.app;

import com.qiqua.springapilens.app.ai.AiAnalysisService;
import com.qiqua.springapilens.app.ai.OpenAiCompatibleClient;
import com.qiqua.springapilens.app.config.AiConfigService;
import com.qiqua.springapilens.app.history.ScanHistoryStore;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

@SpringBootApplication
public class SpringApiLensApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringApiLensApplication.class, args);
    }

    @Bean
    RepositoryScanner repositoryScanner() {
        return new RepositoryScanner();
    }

    @Bean
    AiAnalysisService aiAnalysisService(RestClient.Builder restClientBuilder, AiConfigService aiConfigService) {
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restClientBuilder);
        return new AiAnalysisService(aiConfigService::load, client);
    }

    @Bean
    AiConfigService aiConfigService() {
        String configuredPath = System.getenv("SPRING_API_LENS_AI_CONFIG");
        Path path = configuredPath == null || configuredPath.isBlank()
            ? Path.of(".spring-api-lens", "ai-config.json")
            : Path.of(configuredPath);
        return new AiConfigService(path);
    }

    @Bean
    ScanHistoryStore scanHistoryStore() {
        return new ScanHistoryStore(Path.of(".spring-api-lens", "history"));
    }
}
