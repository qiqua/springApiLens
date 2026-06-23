package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import com.qiqua.springapilens.app.ai.AiAnalysisService;
import com.qiqua.springapilens.app.ai.AiSummary;
import com.qiqua.springapilens.app.config.AiConfigService;
import com.qiqua.springapilens.app.history.ScanHistoryEntry;
import com.qiqua.springapilens.app.history.ScanHistoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    LatestScanStore latestScanStore;

    @MockBean
    RepositoryScanner repositoryScanner;

    @MockBean
    AiAnalysisService aiAnalysisService;

    @MockBean
    ScanHistoryStore scanHistoryStore;

    @MockBean
    AiConfigService aiConfigService;

    @BeforeEach
    void clearLatestScan() {
        latestScanStore.clear();
    }

    @Test
    void returnsEmptyEndpointListBeforeScan() throws Exception {
        mockMvc.perform(get("/api/endpoints"))
            .andExpect(status().isOk());
    }

    @Test
    void workbenchReturnsEmptyPayloadBeforeScan() throws Exception {
        mockMvc.perform(get("/api/workbench"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.endpointCount").value(0))
            .andExpect(jsonPath("$.endpoints").isArray());
    }

    @Test
    void workbenchReturnsLatestScanPayload() throws Exception {
        when(repositoryScanner.scan(any(Path.class))).thenReturn(sampleScanResult());
        when(scanHistoryStore.save(any())).thenReturn(sampleHistoryEntry());

        mockMvc.perform(post("/api/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoPath\":\"D:\\\\workspace\\\\demo\",\"snapshotPath\":\"\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/workbench"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repository.repoName").value("demo"))
            .andExpect(jsonPath("$.summary.endpointCount").value(1))
            .andExpect(jsonPath("$.summary.tableCount").value(1))
            .andExpect(jsonPath("$.endpoints[0].httpMethod").value("POST"))
            .andExpect(jsonPath("$.endpoints[0].key").isString())
            .andExpect(jsonPath("$.endpoints[0].authors[0]").value("Ada"))
            .andExpect(jsonPath("$.filters.authors[0]").value("Ada"))
            .andExpect(jsonPath("$.filters.httpMethods[0]").value("POST"));
    }

    @Test
    void scanSavesHistoryEntry() throws Exception {
        when(repositoryScanner.scan(any(Path.class))).thenReturn(sampleScanResult());
        when(scanHistoryStore.save(any())).thenReturn(sampleHistoryEntry());

        mockMvc.perform(post("/api/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoPath\":\"D:\\\\workspace\\\\demo\",\"snapshotPath\":\"\"}"))
            .andExpect(status().isOk());

        verify(scanHistoryStore).save(sampleScanResult());
    }

    @Test
    void historyListsAndLoadsPreviousScans() throws Exception {
        when(scanHistoryStore.list()).thenReturn(List.of(sampleHistoryEntry()));
        when(scanHistoryStore.load("scan-1")).thenReturn(Optional.of(sampleScanResult()));

        mockMvc.perform(get("/api/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("scan-1"))
            .andExpect(jsonPath("$[0].repoName").value("demo"))
            .andExpect(jsonPath("$[0].endpointCount").value(1));

        mockMvc.perform(post("/api/history/{scanId}/load", "scan-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repoName").value("demo"))
            .andExpect(jsonPath("$.endpointCount").value(1));

        mockMvc.perform(get("/api/workbench"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repository.repoName").value("demo"));
    }

    @Test
    void aiConfigCanBeReadAndSavedWithoutReturningApiKey() throws Exception {
        when(aiConfigService.status()).thenReturn(new AiConfigResponse(true, false, "deepseek", "https://api.deepseek.com", "deepseek-chat", "DEEPSEEK_API_KEY", ""));

        mockMvc.perform(get("/api/ai-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.provider").value("deepseek"))
            .andExpect(jsonPath("$.apiKey").doesNotExist());

        mockMvc.perform(post("/api/ai-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": true,
                      "provider": "local",
                      "baseUrl": "http://127.0.0.1:11434",
                      "model": "qwen",
                      "apiKeyEnv": "LOCAL_AI_KEY"
                    }
                    """))
            .andExpect(status().isOk());

        verify(aiConfigService).save(eq(new AiConfigUpdateRequest(true, "local", "http://127.0.0.1:11434", "qwen", "LOCAL_AI_KEY")));
    }

    @Test
    void endpointDetailReturnsEvidenceForKnownEndpoint() throws Exception {
        when(repositoryScanner.scan(any(Path.class))).thenReturn(sampleScanResult());

        mockMvc.perform(post("/api/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoPath\":\"D:\\\\workspace\\\\demo\",\"snapshotPath\":\"\"}"))
            .andExpect(status().isOk());

        String endpointKey = EndpointKey.from(sampleEndpoint());

        mockMvc.perform(get("/api/endpoints/{endpointKey}", endpointKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endpoint.httpMethod").value("POST"))
            .andExpect(jsonPath("$.callEdges[0].evidence").value("service.create(request)"))
            .andExpect(jsonPath("$.sqlFragments[0].operationType").value("insert"))
            .andExpect(jsonPath("$.tables[0]").value("orders"))
            .andExpect(jsonPath("$.authors[0].name").value("Ada"))
            .andExpect(jsonPath("$.authors[0].lineCount").value(12))
            .andExpect(jsonPath("$.profile.purpose").value("处理 POST /api/orders，对应 OrderController#create。"))
            .andExpect(jsonPath("$.profile.callGuide").value("使用 POST /api/orders 调用，请求体 CreateOrderRequest，响应 ApiResult<OrderVO>。"))
            .andExpect(jsonPath("$.profile.businessFlow[0]").value("OrderController.create() -> OrderService.create()：service.create(request)"))
            .andExpect(jsonPath("$.profile.dataTables[0]").value("orders（insert，OrderMapper.insert）"))
            .andExpect(jsonPath("$.profile.authorSummary[0]").value("Ada 在入口、调用链及相关文件历史贡献 12 条证据，约 75%。"))
            .andExpect(jsonPath("$.profile.risks[0]").value("该接口包含写操作 insert，建议重点关注幂等性、事务边界和参数校验。"))
            .andExpect(jsonPath("$.profile.testSuggestions[0]").value("验证 POST /api/orders 的正常请求、参数缺失和异常分支。"));
    }

    @Test
    void endpointDetailDoesNotIncludeCallEdgesFromMethodsWithSharedPrefix() throws Exception {
        latestScanStore.save(scanResultWithSimilarMethodPrefix());

        mockMvc.perform(get("/api/endpoints/{endpointKey}", EndpointKey.from(sampleEndpoint())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.callEdges.length()").value(0))
            .andExpect(jsonPath("$.tables.length()").value(0));
    }

    @Test
    void endpointDetailReturnsNotFoundForUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/api/endpoints/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Endpoint was not found in the latest scan."));
    }

    @Test
    void endpointAiSummaryReturnsGeneratedAnalysisForKnownEndpoint() throws Exception {
        when(repositoryScanner.scan(any(Path.class))).thenReturn(sampleScanResult());
        when(aiAnalysisService.analyze(any(), any(), any(), any(), any()))
            .thenReturn(new AiSummary(true, true, "local", "qwen", "作者: Ada\n业务逻辑: 创建订单。", ""));

        mockMvc.perform(post("/api/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoPath\":\"D:\\\\workspace\\\\demo\",\"snapshotPath\":\"\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/endpoints/{endpointKey}/ai-summary", EndpointKey.from(sampleEndpoint())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("local"))
            .andExpect(jsonPath("$.model").value("qwen"))
            .andExpect(jsonPath("$.content").value("作者: Ada\n业务逻辑: 创建订单。"));
    }

    private ScanResult sampleScanResult() {
        return new ScanResult(
            new RepositoryInfo(Path.of("D:\\workspace\\demo"), "demo", "main", "abc123", false),
            List.of(sampleEndpoint()),
            List.of(new CodeSymbol(
                "src/main/java/com/demo/OrderService.java",
                "method",
                "OrderService",
                "create",
                "OrderService.create()",
                12,
                30
            )),
            List.of(new CallEdge(
                "OrderController.create()",
                "OrderService.create()",
                0.95,
                "service.create(request)"
            ), new CallEdge(
                "OrderService.create()",
                "OrderMapper.insert()",
                0.95,
                "orderMapper.insert(order)"
            )),
            List.of(new SqlFragment(
                "src/main/resources/mapper/OrderMapper.xml",
                "com.demo.OrderMapper",
                "insert",
                "insert into orders(id) values(#{id})",
                List.of("orders"),
                "insert"
            ))
        );
    }

    private ScanResult scanResultWithSimilarMethodPrefix() {
        return new ScanResult(
            new RepositoryInfo(Path.of("D:\\workspace\\demo"), "demo", "main", "abc123", false),
            List.of(sampleEndpoint()),
            List.of(),
            List.of(new CallEdge(
                "OrderController.createType()",
                "OrderTypeMapper.insert()",
                0.95,
                "typeMapper.insert(type)"
            )),
            List.of(new SqlFragment(
                "src/main/resources/mapper/OrderTypeMapper.xml",
                "com.demo.OrderTypeMapper",
                "insert",
                "insert into order_types(id) values(#{id})",
                List.of("order_types"),
                "insert"
            ))
        );
    }

    private ScanHistoryEntry sampleHistoryEntry() {
        return new ScanHistoryEntry(
            "scan-1",
            Instant.parse("2026-06-22T09:00:00Z"),
            "demo",
            "D:\\workspace\\demo",
            "main",
            "abc123",
            1,
            2,
            1
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
            List.of(new AuthorContribution("Ada", "ada@example.com", 0.75, 12))
        );
    }
}
