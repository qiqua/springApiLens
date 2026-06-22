package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.RepositoryInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsDirectoryWithoutGitMetadata() {
        RepositoryValidator validator = new RepositoryValidator();

        assertThatThrownBy(() -> validator.validate(tempDir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(".git");
    }

    @Test
    void acceptsRepositoryRootAndReadsBasicInfo() throws IOException {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.createDirectories(tempDir.resolve(".git/refs/heads"));
        Files.writeString(tempDir.resolve(".git/refs/heads/main"), "abc123\n");

        RepositoryInfo info = new RepositoryValidator().validate(tempDir);

        assertThat(info.rootPath()).isEqualTo(tempDir.toAbsolutePath().normalize());
        assertThat(info.repoName()).isEqualTo(tempDir.getFileName().toString());
        assertThat(info.currentBranch()).isEqualTo("main");
        assertThat(info.headCommit()).isEqualTo("abc123");
    }

    @Test
    void acceptsLinkedWorktreeGitFile() throws IOException {
        Path commonGit = tempDir.resolve("common.git");
        Path worktree = tempDir.resolve("worktree");
        Files.createDirectories(commonGit.resolve("worktrees/worktree"));
        Files.createDirectories(commonGit.resolve("refs/heads"));
        Files.writeString(commonGit.resolve("worktrees/worktree/HEAD"), "ref: refs/heads/feature\n");
        Files.writeString(commonGit.resolve("refs/heads/feature"), "def456\n");
        Files.createDirectories(worktree);
        Files.writeString(worktree.resolve(".git"), "gitdir: " + commonGit.resolve("worktrees/worktree") + "\n");

        RepositoryInfo info = new RepositoryValidator().validate(worktree);

        assertThat(info.rootPath()).isEqualTo(worktree.toAbsolutePath().normalize());
        assertThat(info.repoName()).isEqualTo("worktree");
        assertThat(info.currentBranch()).isEqualTo("feature");
        assertThat(info.headCommit()).isEqualTo("def456");
    }
}
