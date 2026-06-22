package com.qiqua.springapilens.app.history;

import java.time.Instant;

public record ScanHistoryEntry(
    String id,
    Instant scannedAt,
    String repoName,
    String rootPath,
    String branchName,
    String headCommit,
    int endpointCount,
    int callEdgeCount,
    int sqlFragmentCount
) {
}
