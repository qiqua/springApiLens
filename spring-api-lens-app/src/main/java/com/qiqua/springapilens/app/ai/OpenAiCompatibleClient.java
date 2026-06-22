package com.qiqua.springapilens.app.ai;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

public class OpenAiCompatibleClient implements AiClient {
    private final RestClient restClient;

    public OpenAiCompatibleClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String complete(AiAnalysisRequest request) {
        AiConfig config = request.config();
        ChatCompletionResponse response = restClient.post()
            .uri(normalizedBaseUrl(config.baseUrl()) + "/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + config.apiKey())
            .body(new ChatCompletionRequest(
                config.model(),
                List.of(
                    new ChatMessage("system", "你是严谨的代码接口分析助手。只基于用户给出的代码证据回答。"),
                    new ChatMessage("user", request.prompt())
                ),
                0.2
            ))
            .retrieve()
            .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "";
        }
        ChatMessage message = response.choices().getFirst().message();
        return message == null || message.content() == null ? "" : message.content();
    }

    private String normalizedBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        double temperature
    ) {
    }

    private record ChatMessage(
        String role,
        String content
    ) {
    }

    private record ChatCompletionResponse(
        List<Choice> choices
    ) {
    }

    private record Choice(
        ChatMessage message
    ) {
    }
}
