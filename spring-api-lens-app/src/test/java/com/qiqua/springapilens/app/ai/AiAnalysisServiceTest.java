package com.qiqua.springapilens.app.ai;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceTest {
    @Test
    void buildsEvidencePromptAndReturnsSummary() {
        AtomicReference<AiAnalysisRequest> capturedRequest = new AtomicReference<>();
        AiClient client = request -> {
            capturedRequest.set(request);
            return "作者: Ada\n业务逻辑: 创建订单并写入 orders 表。";
        };
        AiConfig config = new AiConfig(true, "local", "http://127.0.0.1:11434/v1", "qwen", "AI_KEY", "secret");
        AiAnalysisService service = new AiAnalysisService(() -> config, client);

        AiSummary summary = service.analyze(sampleScanResult(), sampleEndpoint(), sampleCallEdges(), sampleSqlFragments(), List.of("orders"));

        assertThat(summary.enabled()).isTrue();
        assertThat(summary.configured()).isTrue();
        assertThat(summary.provider()).isEqualTo("local");
        assertThat(summary.model()).isEqualTo("qwen");
        assertThat(summary.content()).contains("创建订单");
        assertThat(capturedRequest.get().prompt()).contains("POST /api/orders");
        assertThat(capturedRequest.get().prompt()).contains("请用中文输出");
        assertThat(capturedRequest.get().prompt()).contains("接口用途");
        assertThat(capturedRequest.get().prompt()).contains("业务逻辑");
        assertThat(capturedRequest.get().prompt()).contains("Ada <ada@example.com>");
        assertThat(capturedRequest.get().prompt()).contains("OrderMapper.insert");
        assertThat(capturedRequest.get().prompt()).contains("orders");
    }

    @Test
    void returnsDisabledStatusWithoutCallingClientWhenAiIsNotConfigured() {
        AtomicReference<AiAnalysisRequest> capturedRequest = new AtomicReference<>();
        AiAnalysisService service = new AiAnalysisService(AiConfig::disabled, request -> {
            capturedRequest.set(request);
            return "should not be called";
        });

        AiSummary summary = service.analyze(sampleScanResult(), sampleEndpoint(), List.of(), List.of(), List.of());

        assertThat(summary.enabled()).isFalse();
        assertThat(summary.configured()).isFalse();
        assertThat(summary.content()).isBlank();
        assertThat(capturedRequest).hasValue(null);
    }

    private ScanResult sampleScanResult() {
        return new ScanResult(
            new RepositoryInfo(Path.of("D:\\workspace\\demo"), "demo", "main", "abc123", false),
            List.of(sampleEndpoint()),
            List.of(),
            sampleCallEdges(),
            sampleSqlFragments()
        );
    }

    private ApiEndpoint sampleEndpoint() {
        return new ApiEndpoint(
            "src/main/java/com/demo/OrderController.java",
            "OrderController",
            "create",
            "POST",
            "/api/orders",
            "[]",
            "CreateOrderRequest",
            "ApiResult<OrderVO>",
            20,
            36,
            List.of(new AuthorContribution("Ada", "ada@example.com", 1.0, 17))
        );
    }

    private List<CallEdge> sampleCallEdges() {
        return List.of(new CallEdge(
            "OrderController.create()",
            "OrderService.create()",
            0.95,
            "service.create(request)"
        ));
    }

    private List<SqlFragment> sampleSqlFragments() {
        return List.of(new SqlFragment(
            "src/main/resources/mapper/OrderMapper.xml",
            "com.demo.OrderMapper",
            "insert",
            "insert into orders(id) values(#{id})",
            List.of("orders"),
            "insert"
        ));
    }
}
