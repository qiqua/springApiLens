package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
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

    @Test
    void includesDownstreamMethodAuthorsInEndpointAuthorContributions() throws Exception {
        initGit();
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                private final OrderService orderService;
                OrderController(OrderService orderService) { this.orderService = orderService; }
                @GetMapping("/detail")
                String detail() { return orderService.detail(); }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "controller");

        configureGit("Li Ming", "liming@example.com");
        write("src/main/java/com/example/OrderService.java", """
            package com.example;
            class OrderService {
                String detail() { return "ok"; }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "service");

        ScanResult result = new RepositoryScanner().scan(repoRoot);

        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().getFirst().authors())
            .extracting("name")
            .contains("Zhang San", "Li Ming");
    }

    @Test
    void includesAuthorsWhoTouchedEndpointFileHistory() throws Exception {
        initGit();
        configureGit("Li Ming", "liming@example.com");
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            class OrderController {
                String seed() { return "seed"; }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "seed controller file");

        configureGit("Zhang San", "zhang@example.com");
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                @GetMapping("/detail")
                String detail() { return "ok"; }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "add endpoint");

        ScanResult result = new RepositoryScanner().scan(repoRoot);

        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().getFirst().authors())
            .extracting("name")
            .contains("Zhang San", "Li Ming");
    }

    @Test
    void doesNotMixDownstreamAuthorsFromMethodsWithSharedPrefix() throws Exception {
        initGit();
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                private final OrderTypeService orderTypeService;
                OrderController(OrderTypeService orderTypeService) { this.orderTypeService = orderTypeService; }
                @GetMapping("/detail")
                String detail() { return "ok"; }
                @GetMapping("/detail/type")
                String detailType() { return orderTypeService.detailType(); }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "controller");

        configureGit("Li Ming", "liming@example.com");
        write("src/main/java/com/example/OrderTypeService.java", """
            package com.example;
            class OrderTypeService {
                String detailType() { return "type"; }
            }
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "type service");

        ScanResult result = new RepositoryScanner().scan(repoRoot);

        ApiEndpoint detail = result.endpoints().stream()
            .filter(endpoint -> endpoint.methodName().equals("detail"))
            .findFirst()
            .orElseThrow();
        assertThat(detail.authors())
            .extracting("name")
            .doesNotContain("Li Ming");
    }

    private void initGit() throws Exception {
        run("git", "init");
        configureGit("Zhang San", "zhang@example.com");
    }

    private void configureGit(String name, String email) throws Exception {
        run("git", "config", "user.name", name);
        run("git", "config", "user.email", email);
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
