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

    @Test
    void keepsRepositoryAndMapperCallsWhenMethodIsInheritedFromFrameworkBaseType() throws IOException {
        Path service = write("src/main/java/com/example/TipDataService.java", """
            package com.example;
            class TipDataService {
                private final TipDataMapper tipDataMapper;
                private final AuditLogRepository auditLogRepository;
                TipDataService(TipDataMapper tipDataMapper, AuditLogRepository auditLogRepository) {
                    this.tipDataMapper = tipDataMapper;
                    this.auditLogRepository = auditLogRepository;
                }
                void load() {
                    tipDataMapper.selectList(null);
                    auditLogRepository.findById(1L);
                }
            }
            """);
        Path mapper = write("src/main/java/com/example/TipDataMapper.java", """
            package com.example;
            interface TipDataMapper {
            }
            """);
        Path repository = write("src/main/java/com/example/AuditLogRepository.java", """
            package com.example;
            interface AuditLogRepository {
            }
            """);

        List<Path> files = List.of(service, mapper, repository);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> edges = new CallEdgeExtractor().extract(repoRoot, files, symbols);

        assertThat(edges)
            .extracting(edge -> edge.fromSignature() + " -> " + edge.toSignature())
            .contains(
                "TipDataService.load() -> TipDataMapper.selectList()",
                "TipDataService.load() -> AuditLogRepository.findById()"
            );
    }

    @Test
    void extractsCallsFromMethodsWithAnnotatedAndMultilineParameters() throws IOException {
        Path controller = write("src/main/java/com/example/DataExportController.java", """
            package com.example;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestParam;
            import java.util.List;

            class DataExportController {
                @Autowired
                private TipDataService tipDataService;
                @Autowired
                private InfoDataExcelService infoDataExcelService;
                @Autowired
                private ExportService exportService;

                public void exportSelected(@RequestParam("type") String type, @RequestBody List<Long> ids) {
                    tipDataService.exportSelected(ids);
                }

                public void quarantineRegistrationListExportSelected(@RequestBody List<Long> ids, HttpServletResponse response) {
                    exportService.exportSelected(response, ids);
                }

                public void exportInfoDataExcel(@PathVariable("typeId") Integer typeId,
                                                HttpServletResponse response) {
                    byte[] excelData = infoDataExcelService.exportExcel(Long.valueOf(typeId));
                }
            }
            """);
        Path tipDataService = write("src/main/java/com/example/TipDataService.java", """
            package com.example;
            class TipDataService {
                void exportSelected(java.util.List<Long> ids) {}
            }
            """);
        Path infoDataExcelService = write("src/main/java/com/example/InfoDataExcelService.java", """
            package com.example;
            class InfoDataExcelService {
                byte[] exportExcel(Long typeId) { return new byte[0]; }
            }
            """);
        Path exportService = write("src/main/java/com/example/ExportService.java", """
            package com.example;
            import java.io.IOException;
            class ExportService {
                private TipQuarantineRegistrationListRepository repository;
                public void exportSelected(HttpServletResponse response, java.util.List<Long> ids) throws IOException {
                    repository.findAllById(ids);
                }
            }
            """);
        Path repository = write("src/main/java/com/example/TipQuarantineRegistrationListRepository.java", """
            package com.example;
            interface TipQuarantineRegistrationListRepository {
            }
            """);

        List<Path> files = List.of(controller, tipDataService, infoDataExcelService, exportService, repository);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> edges = new CallEdgeExtractor().extract(repoRoot, files, symbols);

        assertThat(edges)
            .extracting(edge -> edge.fromSignature() + " -> " + edge.toSignature())
            .contains(
                "DataExportController.exportSelected() -> TipDataService.exportSelected()",
                "DataExportController.quarantineRegistrationListExportSelected() -> ExportService.exportSelected()",
                "ExportService.exportSelected() -> TipQuarantineRegistrationListRepository.findAllById()",
                "DataExportController.exportInfoDataExcel() -> InfoDataExcelService.exportExcel()"
            );
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
