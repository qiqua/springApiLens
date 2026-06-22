package com.qiqua.springapilens.core.model;

import java.nio.file.Path;

public record RepositoryInfo(
    Path rootPath,
    String repoName,
    String currentBranch,
    String headCommit,
    boolean hasUncommittedChanges
) {
}
