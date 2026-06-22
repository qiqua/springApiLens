package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScanResultRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsEndpointSnapshot() {
        Path snapshotPath = tempDir.resolve("scan-result.tsv");
        ScanResultRepository repository = new ScanResultRepository(snapshotPath);
        ScanResult result = new ScanResult(
            new RepositoryInfo(tempDir, "demo", "main", "abc123", false),
            List.of(new ApiEndpoint("A.java", "AController", "create", "POST", "/a", "[]", "Request", "Response", 1, 5)),
            List.of(new CodeSymbol("A.java", "METHOD", "AController", "create", "AController.create()", 1, 5)),
            List.of(new CallEdge("AController.create()", "AService.create()", 0.95, "aService.create")),
            List.of(new SqlFragment("A.xml", "AMapper", "insertA", "insert into a_table values (?)", List.of("a_table"), "insert"))
        );

        repository.save(result);

        assertThat(repository.listEndpoints()).singleElement().satisfies(endpoint -> {
            assertThat(endpoint.path()).isEqualTo("/a");
            assertThat(endpoint.httpMethod()).isEqualTo("POST");
            assertThat(endpoint.className()).isEqualTo("AController");
        });
    }
}
