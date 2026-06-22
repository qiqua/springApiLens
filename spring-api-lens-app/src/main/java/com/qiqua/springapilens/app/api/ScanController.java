package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import com.qiqua.springapilens.core.scanner.ScanResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ScanController {
    private final List<ApiEndpoint> inMemoryEndpoints = new ArrayList<>();

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        ScanResult result = new RepositoryScanner().scan(Path.of(request.repoPath()));
        if (request.snapshotPath() != null && !request.snapshotPath().isBlank()) {
            new ScanResultRepository(Path.of(request.snapshotPath())).save(result);
        }
        inMemoryEndpoints.clear();
        inMemoryEndpoints.addAll(result.endpoints());
        return new ScanResponse(
            result.repositoryInfo().repoName(),
            result.endpoints().size(),
            result.callEdges().size(),
            result.sqlFragments().size()
        );
    }

    @GetMapping("/endpoints")
    public List<EndpointResponse> endpoints() {
        return inMemoryEndpoints.stream()
            .map(endpoint -> new EndpointResponse(
                endpoint.httpMethod(),
                endpoint.path(),
                endpoint.className(),
                endpoint.methodName(),
                endpoint.requestBodyType(),
                endpoint.responseType()
            ))
            .toList();
    }
}
