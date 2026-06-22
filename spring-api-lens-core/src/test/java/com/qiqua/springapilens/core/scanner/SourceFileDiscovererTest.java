package com.qiqua.springapilens.core.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFileDiscovererTest {
    @TempDir
    Path repoRoot;

    @Test
    void discoversJavaAndResourceFilesAndSkipsBuildOutput() throws IOException {
        write("src/main/java/com/example/OrderController.java");
        write("src/main/resources/mapper/OrderMapper.xml");
        write("src/main/resources/application.yml");
        write("target/generated/Generated.java");
        write("build/tmp/Other.java");

        List<Path> files = new SourceFileDiscoverer().discover(repoRoot);

        assertThat(files)
            .extracting(path -> repoRoot.relativize(path).toString().replace('\\', '/'))
            .containsExactly(
                "src/main/java/com/example/OrderController.java",
                "src/main/resources/application.yml",
                "src/main/resources/mapper/OrderMapper.xml"
            );
    }

    private void write(String relativePath) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "");
    }
}
