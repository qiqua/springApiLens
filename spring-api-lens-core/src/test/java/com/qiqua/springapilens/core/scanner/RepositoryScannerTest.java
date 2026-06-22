package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryScannerTest {
    @TempDir
    Path repoRoot;

    @Test
    void scansEndpointCallEdgeAndSqlFragment() throws Exception {
        initGit();
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                private final OrderService orderService;
                OrderController(OrderService orderService) { this.orderService = orderService; }
                @PostMapping("/create")
                String create() { orderService.createOrder(); return "ok"; }
            }
            """);
        write("src/main/java/com/example/OrderService.java", """
            package com.example;
            class OrderService {
                private final OrderMapper orderMapper;
                OrderService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }
                void createOrder() { orderMapper.insertOrder(); }
            }
            """);
        write("src/main/java/com/example/OrderMapper.java", """
            package com.example;
            interface OrderMapper { void insertOrder(); }
            """);
        write("src/main/resources/mapper/OrderMapper.xml", """
            <mapper namespace="com.example.OrderMapper">
              <insert id="insertOrder">insert into order_main (id) values (#{id})</insert>
            </mapper>
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "fixture");

        ScanResult result = new RepositoryScanner().scan(repoRoot);

        assertThat(result.repositoryInfo().repoName()).isEqualTo(repoRoot.getFileName().toString());
        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().getFirst().authors())
            .singleElement()
            .satisfies(author -> {
                assertThat(author.name()).isEqualTo("Zhang San");
                assertThat(author.email()).isEqualTo("zhang@example.com");
            });
        assertThat(result.callEdges()).hasSize(2);
        assertThat(result.sqlFragments()).hasSize(1);
    }

    private void initGit() throws Exception {
        run("git", "init");
        run("git", "config", "user.name", "Zhang San");
        run("git", "config", "user.email", "zhang@example.com");
    }

    private void write(String relativePath, String content) throws Exception {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
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
