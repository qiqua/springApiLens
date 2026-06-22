package com.qiqua.springapilens.core.scanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class SourceFileDiscoverer {
    public List<Path> discover(Path repoRoot) {
        Path normalized = repoRoot.toAbsolutePath().normalize();
        try (var stream = Files.walk(normalized)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> isSupported(normalized, path))
                .sorted(Comparator.comparing(path -> normalized.relativize(path).toString()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to discover source files under " + normalized, e);
        }
    }

    private boolean isSupported(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (relative.startsWith("target/")
            || relative.startsWith("build/")
            || relative.startsWith(".idea/")
            || relative.startsWith(".gradle/")
            || relative.startsWith("node_modules/")) {
            return false;
        }
        return relative.startsWith("src/main/java/") && relative.endsWith(".java")
            || relative.startsWith("src/main/resources/") && (
                relative.endsWith(".xml")
                    || relative.endsWith(".yml")
                    || relative.endsWith(".yaml")
                    || relative.endsWith(".properties")
            );
    }
}
