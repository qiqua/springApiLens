package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.AuthorContribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GitBlameAnalyzer {
    public List<AuthorContribution> analyze(Path repoRoot, Path file, int lineStart, int lineEnd) {
        String relativeFile = repoRoot.toAbsolutePath().normalize()
            .relativize(file.toAbsolutePath().normalize())
            .toString()
            .replace('\\', '/');
        String output = run(repoRoot, "git", "blame", "--line-porcelain", "-L", lineStart + "," + lineEnd, "--", relativeFile);
        Map<String, MutableContribution> counts = new LinkedHashMap<>();
        String currentAuthor = "";
        String currentEmail = "";
        for (String line : output.split("\\R")) {
            if (line.startsWith("author ")) {
                currentAuthor = line.substring("author ".length());
            } else if (line.startsWith("author-mail ")) {
                currentEmail = line.substring("author-mail ".length()).replace("<", "").replace(">", "");
            } else if (line.startsWith("\t")) {
                String author = currentAuthor;
                String email = currentEmail;
                String key = currentAuthor + "\n" + currentEmail;
                counts.computeIfAbsent(key, ignored -> new MutableContribution(author, email)).lineCount++;
            }
        }
        int total = counts.values().stream().mapToInt(value -> value.lineCount).sum();
        List<AuthorContribution> contributions = new ArrayList<>();
        for (MutableContribution value : counts.values()) {
            contributions.add(new AuthorContribution(
                value.name,
                value.email,
                total == 0 ? 0.0 : (double) value.lineCount / total,
                value.lineCount
            ));
        }
        return contributions;
    }

    private String run(Path repoRoot, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalArgumentException(String.join(" ", command) + " failed: " + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + String.join(" ", command), e);
        }
    }

    private static final class MutableContribution {
        private final String name;
        private final String email;
        private int lineCount;

        private MutableContribution(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
