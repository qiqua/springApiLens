package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CallEdgeExtractorTest {
    @TempDir
    Path repoRoot;

    @Test
    void extractsControllerToServiceAndServiceToMapperEdges() throws IOException {
        Path controller = write("src/main/java/com/example/OrderController.java", """
            package com.example;
            class OrderController {
                private final OrderService orderService;
                OrderController(OrderService orderService) { this.orderService = orderService; }
                void create() { orderService.createOrder(); }
            }
            """);
        Path service = write("src/main/java/com/example/OrderService.java", """
            package com.example;
            class OrderService {
                private final OrderMapper orderMapper;
                OrderService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }
                void createOrder() { orderMapper.insertOrder(); }
            }
            """);
        Path mapper = write("src/main/java/com/example/OrderMapper.java", """
            package com.example;
            interface OrderMapper {
                void insertOrder();
            }
            """);

        List<Path> files = List.of(controller, service, mapper);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> edges = new CallEdgeExtractor().extract(repoRoot, files, symbols);

        assertThat(edges)
            .extracting(edge -> edge.fromSignature() + " -> " + edge.toSignature())
            .contains(
                "OrderController.create() -> OrderService.createOrder()",
                "OrderService.createOrder() -> OrderMapper.insertOrder()"
            );
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
