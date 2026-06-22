package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryValidator {
    public RepositoryInfo validate(Path rootPath) {
        Path normalized = rootPath.toAbsolutePath().normalize();
        GitPaths gitPaths = resolveGitPaths(normalized);
        if (gitPaths == null) {
            throw new IllegalArgumentException("Repository root must contain .git: " + normalized);
        }
        String branch = readBranch(gitPaths.worktreeGitDir());
        String headCommit = readHeadCommit(gitPaths.commonGitDir(), gitPaths.worktreeGitDir(), branch);
        return new RepositoryInfo(
            normalized,
            normalized.getFileName().toString(),
            branch,
            headCommit,
            false
        );
    }

    private GitPaths resolveGitPaths(Path rootPath) {
        Path dotGit = rootPath.resolve(".git");
        if (Files.isDirectory(dotGit)) {
            return new GitPaths(dotGit, dotGit);
        }
        if (!Files.isRegularFile(dotGit)) {
            return null;
        }
        try {
            String content = Files.readString(dotGit).trim();
            if (!content.startsWith("gitdir:")) {
                return null;
            }
            Path worktreeGitDir = Path.of(content.substring("gitdir:".length()).trim())
                .toAbsolutePath()
                .normalize();
            return new GitPaths(worktreeGitDir, resolveCommonGitDir(worktreeGitDir));
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveCommonGitDir(Path worktreeGitDir) {
        Path commonDir = worktreeGitDir.resolve("commondir");
        if (Files.isRegularFile(commonDir)) {
            try {
                Path raw = Path.of(Files.readString(commonDir).trim());
                if (!raw.isAbsolute()) {
                    raw = worktreeGitDir.resolve(raw);
                }
                return raw.toAbsolutePath().normalize();
            } catch (IOException e) {
                return worktreeGitDir;
            }
        }
        Path parent = worktreeGitDir.getParent();
        if (parent != null && "worktrees".equals(parent.getFileName().toString()) && parent.getParent() != null) {
            return parent.getParent().toAbsolutePath().normalize();
        }
        return worktreeGitDir;
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

    private String readHeadCommit(Path commonGitDir, Path worktreeGitDir, String branch) {
        if ("UNKNOWN".equals(branch) || "DETACHED".equals(branch)) {
            return "UNKNOWN";
        }
        Path ref = commonGitDir.resolve("refs").resolve("heads").resolve(branch);
        try {
            return Files.readString(ref).trim();
        } catch (IOException e) {
            try {
                return Files.readString(worktreeGitDir.resolve("HEAD")).trim();
            } catch (IOException ignored) {
                return "UNKNOWN";
            }
        }
    }

    private record GitPaths(Path worktreeGitDir, Path commonGitDir) {
    }
}
