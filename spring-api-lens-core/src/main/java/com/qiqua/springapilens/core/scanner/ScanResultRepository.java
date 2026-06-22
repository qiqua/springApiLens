package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.ScanResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScanResultRepository {
    private final Path snapshotPath;

    public ScanResultRepository(Path snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public void save(ScanResult result) {
        try {
            if (snapshotPath.getParent() != null) {
                Files.createDirectories(snapshotPath.getParent());
            }
            List<String> lines = new ArrayList<>();
            for (ApiEndpoint endpoint : result.endpoints()) {
                lines.add(String.join("\t",
                    escape(endpoint.relativeFile()),
                    escape(endpoint.className()),
                    escape(endpoint.methodName()),
                    escape(endpoint.httpMethod()),
                    escape(endpoint.path()),
                    escape(endpoint.requestParamsJson()),
                    escape(endpoint.requestBodyType()),
                    escape(endpoint.responseType()),
                    Integer.toString(endpoint.lineStart()),
                    Integer.toString(endpoint.lineEnd())
                ));
            }
            Files.write(snapshotPath, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save scan result to " + snapshotPath, e);
        }
    }

    public List<ApiEndpoint> listEndpoints() {
        if (!Files.exists(snapshotPath)) {
            return List.of();
        }
        try {
            List<ApiEndpoint> endpoints = new ArrayList<>();
            for (String line : Files.readAllLines(snapshotPath)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                endpoints.add(new ApiEndpoint(
                    unescape(parts[0]),
                    unescape(parts[1]),
                    unescape(parts[2]),
                    unescape(parts[3]),
                    unescape(parts[4]),
                    unescape(parts[5]),
                    unescape(parts[6]),
                    unescape(parts[7]),
                    Integer.parseInt(parts[8]),
                    Integer.parseInt(parts[9])
                ));
            }
            return endpoints;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load scan result from " + snapshotPath, e);
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }
}
