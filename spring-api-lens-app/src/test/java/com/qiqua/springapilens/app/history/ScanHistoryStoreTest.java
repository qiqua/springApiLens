package com.qiqua.springapilens.app.history;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScanHistoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesListsAndLoadsScanResults() {
        ScanHistoryStore store = new ScanHistoryStore(tempDir.resolve("history"));
        ScanHistoryEntry entry = store.save(sampleScanResult());

        assertThat(entry.id()).isNotBlank();
        assertThat(entry.repoName()).isEqualTo("demo");
        assertThat(entry.endpointCount()).isEqualTo(1);
        assertThat(store.list()).singleElement().satisfies(saved -> {
            assertThat(saved.id()).isEqualTo(entry.id());
            assertThat(saved.branchName()).isEqualTo("main");
            assertThat(saved.headCommit()).isEqualTo("abc123");
        });
        assertThat(store.load(entry.id())).get().satisfies(result ->
            assertThat(result.endpoints()).singleElement().satisfies(endpoint ->
                assertThat(endpoint.path()).isEqualTo("/api/orders")
            )
        );
    }

    private ScanResult sampleScanResult() {
        return new ScanResult(
            new RepositoryInfo(Path.of("D:\\workspace\\demo"), "demo", "main", "abc123", false),
            List.of(new ApiEndpoint(
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
            )),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
