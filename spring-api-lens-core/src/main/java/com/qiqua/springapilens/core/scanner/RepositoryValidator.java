package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryValidator {
    public RepositoryInfo validate(Path rootPath) {
        Path normalized = rootPath.toAbsolutePath().normalize();
        Path gitDir = normalized.resolve(".git");
        if (!Files.isDirectory(gitDir)) {
            throw new IllegalArgumentException("Repository root must contain .git: " + normalized);
        }
        String branch = readBranch(gitDir);
        String headCommit = readHeadCommit(gitDir, branch);
        return new RepositoryInfo(
            normalized,
            normalized.getFileName().toString(),
            branch,
            headCommit,
            false
        );
    }

    private String readBranch(Path gitDir) {
        Path head = gitDir.resolve("HEAD");
        try {
            String content = Files.readString(head).trim();
            if (content.startsWith("ref: refs/heads/")) {
                return content.substring("ref: refs/heads/".length());
            }
            return "DETACHED";
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }

    private String readHeadCommit(Path gitDir, String branch) {
        if ("UNKNOWN".equals(branch) || "DETACHED".equals(branch)) {
            return "UNKNOWN";
        }
        Path ref = gitDir.resolve("refs").resolve("heads").resolve(branch);
        try {
            return Files.readString(ref).trim();
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}
