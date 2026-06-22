package com.qiqua.springapilens.app.api;

public record ScanResponse(String repoName, int endpointCount, int callEdgeCount, int sqlFragmentCount) {
}
