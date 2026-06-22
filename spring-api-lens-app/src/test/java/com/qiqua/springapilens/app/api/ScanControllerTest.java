package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
            .andExpect(jsonPath("$.filters.httpMethods[0]").value("POST"));
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
            .andExpect(jsonPath("$.tables[0]").value("orders"));
    }

    @Test
    void endpointDetailReturnsNotFoundForUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/api/endpoints/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Endpoint was not found in the latest scan."));
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
            36
        );
    }
}
