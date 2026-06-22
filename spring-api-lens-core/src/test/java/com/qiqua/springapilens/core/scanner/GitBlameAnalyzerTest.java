package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.AuthorContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitBlameAnalyzerTest {
    @TempDir
    Path repoRoot;

    @Test
    void calculatesAuthorRatioForLineRange() throws Exception {
        run("git", "init");
        run("git", "config", "user.name", "Zhang San");
        run("git", "config", "user.email", "zhang@example.com");
        Path source = repoRoot.resolve("OrderController.java");
        Files.writeString(source, "line1\nline2\n");
        run("git", "add", "OrderController.java");
        run("git", "commit", "-m", "first");

        run("git", "config", "user.name", "Li Si");
        run("git", "config", "user.email", "li@example.com");
        Files.writeString(source, "line1\nline2 changed\n");
        run("git", "add", "OrderController.java");
        run("git", "commit", "-m", "second");

        List<AuthorContribution> contributions = new GitBlameAnalyzer().analyze(repoRoot, source, 1, 2);

        assertThat(contributions).hasSize(2);
        assertThat(contributions)
            .extracting(AuthorContribution::name)
            .containsExactly("Zhang San", "Li Si");
        assertThat(contributions)
            .extracting(AuthorContribution::ratio)
            .containsExactly(0.5, 0.5);
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
            .directory(repoRoot.toFile())
            .redirectErrorStream(true)
            .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new AssertionError(String.join(" ", command) + " failed: " + output);
        }
    }
}
