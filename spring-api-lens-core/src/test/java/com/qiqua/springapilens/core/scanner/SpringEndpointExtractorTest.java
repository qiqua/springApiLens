package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringEndpointExtractorTest {
    @TempDir
    Path repoRoot;

    @Test
    void extractsEndpointFromControllerClassAndMethodMappings() throws IOException {
        Path source = writeJava("""
            package com.example.order;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                @PostMapping("/create")
                ApiResult<OrderVO> createOrder(@RequestBody CreateOrderRequest request) {
                    return null;
                }
            }
            """);

        List<ApiEndpoint> endpoints = new SpringEndpointExtractor().extract(repoRoot, List.of(source));

        assertThat(endpoints).singleElement().satisfies(endpoint -> {
            assertThat(endpoint.className()).isEqualTo("OrderController");
            assertThat(endpoint.methodName()).isEqualTo("createOrder");
            assertThat(endpoint.httpMethod()).isEqualTo("POST");
            assertThat(endpoint.path()).isEqualTo("/api/order/create");
            assertThat(endpoint.requestBodyType()).isEqualTo("CreateOrderRequest");
            assertThat(endpoint.responseType()).isEqualTo("ApiResult<OrderVO>");
            assertThat(endpoint.relativeFile()).isEqualTo("src/main/java/com/example/order/OrderController.java");
            assertThat(endpoint.lineStart()).isLessThan(endpoint.lineEnd());
        });
    }

    private Path writeJava(String content) throws IOException {
        Path source = repoRoot.resolve("src/main/java/com/example/order/OrderController.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, content);
        return source;
    }
}
