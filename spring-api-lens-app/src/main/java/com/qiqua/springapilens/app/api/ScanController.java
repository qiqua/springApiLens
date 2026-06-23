package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.app.ai.AiAnalysisService;
import com.qiqua.springapilens.app.ai.AiSummary;
import com.qiqua.springapilens.app.config.AiConfigService;
import com.qiqua.springapilens.app.history.ScanHistoryEntry;
import com.qiqua.springapilens.app.history.ScanHistoryStore;
import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import com.qiqua.springapilens.core.scanner.ScanResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ScanController {
    private final RepositoryScanner repositoryScanner;
    private final LatestScanStore latestScanStore;
    private final AiAnalysisService aiAnalysisService;
    private final ScanHistoryStore scanHistoryStore;
    private final AiConfigService aiConfigService;
    private final EndpointBusinessProfileBuilder businessProfileBuilder = new EndpointBusinessProfileBuilder();

    public ScanController(
        RepositoryScanner repositoryScanner,
        LatestScanStore latestScanStore,
        AiAnalysisService aiAnalysisService,
        ScanHistoryStore scanHistoryStore,
        AiConfigService aiConfigService
    ) {
        this.repositoryScanner = repositoryScanner;
        this.latestScanStore = latestScanStore;
        this.aiAnalysisService = aiAnalysisService;
        this.scanHistoryStore = scanHistoryStore;
        this.aiConfigService = aiConfigService;
    }

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        ScanResult result = repositoryScanner.scan(Path.of(request.repoPath()));
        if (request.snapshotPath() != null && !request.snapshotPath().isBlank()) {
            new ScanResultRepository(Path.of(request.snapshotPath())).save(result);
        }
        latestScanStore.save(result);
        scanHistoryStore.save(result);
        return new ScanResponse(
            result.repositoryInfo().repoName(),
            result.endpoints().size(),
            result.callEdges().size(),
            result.sqlFragments().size()
        );
    }

    @GetMapping("/endpoints")
    public List<EndpointResponse> endpoints() {
        return latestScanStore.latest()
            .map(ScanResult::endpoints)
            .orElseGet(List::of)
            .stream()
            .map(endpoint -> new EndpointResponse(
                endpoint.httpMethod(),
                endpoint.path(),
                endpoint.className(),
                endpoint.methodName(),
                endpoint.requestBodyType(),
                endpoint.responseType()
            ))
            .toList();
    }

    @GetMapping("/workbench")
    public WorkbenchResponse workbench() {
        return latestScanStore.latest()
            .map(this::toWorkbenchResponse)
            .orElseGet(this::emptyWorkbenchResponse);
    }

    @GetMapping("/endpoints/{endpointKey}")
    public ResponseEntity<?> endpointDetail(@PathVariable("endpointKey") String endpointKey) {
        if (latestScanStore.latest().isEmpty()) {
            return endpointNotFound();
        }

        ScanResult result = latestScanStore.latest().get();
        return result.endpoints().stream()
            .filter(endpoint -> EndpointKey.matches(endpoint, endpointKey))
            .findFirst()
            .<ResponseEntity<?>>map(endpoint -> ResponseEntity.ok(toEndpointDetailResponse(result, endpoint)))
            .orElseGet(this::endpointNotFound);
    }

    @GetMapping("/history")
    public List<ScanHistoryEntry> history() {
        return scanHistoryStore.list();
    }

    @PostMapping("/history/{scanId}/load")
    public ResponseEntity<?> loadHistory(@PathVariable("scanId") String scanId) {
        return scanHistoryStore.load(scanId)
            .<ResponseEntity<?>>map(result -> {
                latestScanStore.save(result);
                return ResponseEntity.ok(new ScanResponse(
                    result.repositoryInfo().repoName(),
                    result.endpoints().size(),
                    result.callEdges().size(),
                    result.sqlFragments().size()
                ));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("Scan history entry was not found.")));
    }

    @GetMapping("/ai-config")
    public AiConfigResponse aiConfig() {
        return aiConfigService.status();
    }

    @PostMapping("/ai-config")
    public AiConfigResponse saveAiConfig(@RequestBody AiConfigUpdateRequest request) {
        return aiConfigService.save(request);
    }

    @PostMapping("/endpoints/{endpointKey}/ai-summary")
    public ResponseEntity<?> endpointAiSummary(@PathVariable("endpointKey") String endpointKey) {
        if (latestScanStore.latest().isEmpty()) {
            return endpointNotFound();
        }

        ScanResult result = latestScanStore.latest().get();
        return result.endpoints().stream()
            .filter(endpoint -> EndpointKey.matches(endpoint, endpointKey))
            .findFirst()
            .<ResponseEntity<?>>map(endpoint -> {
                List<CallEdge> callEdges = relatedCallEdges(result, endpoint);
                List<SqlFragment> sqlFragments = relatedSqlFragments(result, endpoint);
                AiSummary summary = aiAnalysisService.analyze(result, endpoint, callEdges, sqlFragments, tableNames(sqlFragments));
                return ResponseEntity.ok(summary);
            })
            .orElseGet(this::endpointNotFound);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    private WorkbenchResponse emptyWorkbenchResponse() {
        return new WorkbenchResponse(
            new WorkbenchResponse.RepositoryView("", "", "", "", false),
            new WorkbenchResponse.SummaryView(0, 0, 0, 0),
            List.of(),
            new WorkbenchResponse.FilterView(List.of(), List.of(), List.of())
        );
    }

    private WorkbenchResponse toWorkbenchResponse(ScanResult result) {
        RepositoryInfo repositoryInfo = result.repositoryInfo();
        List<String> tables = tableNames(result.sqlFragments());
        List<WorkbenchResponse.EndpointListItem> endpoints = result.endpoints().stream()
            .map(endpoint -> new WorkbenchResponse.EndpointListItem(
                EndpointKey.from(endpoint),
                endpoint.httpMethod(),
                endpoint.path(),
                endpoint.className(),
                endpoint.methodName(),
                endpoint.requestBodyType(),
                endpoint.responseType(),
                endpoint.relativeFile(),
                endpoint.lineStart(),
                endpoint.lineEnd(),
                tableNames(relatedSqlFragments(result, endpoint)),
                relatedCallEdges(result, endpoint).size(),
                authorNames(endpoint)
            ))
            .toList();

        List<String> httpMethods = result.endpoints().stream()
            .map(ApiEndpoint::httpMethod)
            .distinct()
            .sorted()
            .toList();

        return new WorkbenchResponse(
            new WorkbenchResponse.RepositoryView(
                repositoryInfo.repoName(),
                repositoryInfo.rootPath().toString(),
                repositoryInfo.currentBranch(),
                repositoryInfo.headCommit(),
                repositoryInfo.hasUncommittedChanges()
            ),
            new WorkbenchResponse.SummaryView(
                result.endpoints().size(),
                result.callEdges().size(),
                result.sqlFragments().size(),
                tables.size()
            ),
            endpoints,
            new WorkbenchResponse.FilterView(httpMethods, tables, authorFilters(result))
        );
    }

    private EndpointDetailResponse toEndpointDetailResponse(ScanResult result, ApiEndpoint endpoint) {
        List<CallEdge> callEdges = relatedCallEdges(result, endpoint);
        List<SqlFragment> sqlFragments = relatedSqlFragments(result, endpoint);
        return new EndpointDetailResponse(
            new EndpointDetailResponse.EndpointView(
                EndpointKey.from(endpoint),
                endpoint.httpMethod(),
                endpoint.path(),
                endpoint.className(),
                endpoint.methodName(),
                endpoint.requestParamsJson(),
                endpoint.requestBodyType(),
                endpoint.responseType(),
                endpoint.relativeFile(),
                endpoint.lineStart(),
                endpoint.lineEnd()
            ),
            businessProfileBuilder.build(endpoint, callEdges, sqlFragments),
            callEdges.stream()
                .map(edge -> new EndpointDetailResponse.CallEdgeView(
                    edge.fromSignature(),
                    edge.toSignature(),
                    edge.confidence(),
                    edge.evidence()
                ))
                .toList(),
            sqlFragments.stream()
                .map(fragment -> new EndpointDetailResponse.SqlFragmentView(
                    fragment.relativeFile(),
                    fragment.mapperNamespace(),
                    fragment.mapperMethod(),
                    fragment.sqlText(),
                    fragment.tables(),
                    fragment.operationType()
                ))
                .toList(),
            tableNames(sqlFragments),
            endpoint.authors().stream()
                .map(author -> new EndpointDetailResponse.AuthorView(
                    author.name(),
                    author.email(),
                    author.ratio(),
                    author.lineCount()
                ))
                .toList()
        );
    }

    private ResponseEntity<ApiErrorResponse> endpointNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiErrorResponse("Endpoint was not found in the latest scan."));
    }

    private List<CallEdge> relatedCallEdges(ScanResult result, ApiEndpoint endpoint) {
        String controllerMethod = endpoint.className() + "." + endpoint.methodName();
        Set<String> frontier = new LinkedHashSet<>();
        Set<CallEdge> related = new LinkedHashSet<>();
        frontier.add(controllerMethod);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (CallEdge edge : result.callEdges()) {
                if (!related.contains(edge) && matchesAny(edge.fromSignature(), frontier)) {
                    related.add(edge);
                    frontier.add(normalizedSignature(edge.toSignature()));
                    changed = true;
                }
            }
        }
        return List.copyOf(related);
    }

    private boolean matchesAny(String signature, Set<String> methodNames) {
        return methodNames.contains(normalizedSignature(signature));
    }

    private String normalizedSignature(String signature) {
        int parameterStart = signature.indexOf('(');
        if (parameterStart > 0) {
            return signature.substring(0, parameterStart);
        }
        return signature;
    }

    private List<SqlFragment> relatedSqlFragments(ScanResult result, ApiEndpoint endpoint) {
        Set<String> relatedText = relatedCallEdges(result, endpoint).stream()
            .flatMap(edge -> List.of(edge.fromSignature(), edge.toSignature(), edge.evidence()).stream())
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        return result.sqlFragments().stream()
            .filter(fragment -> isRelatedSqlFragment(fragment, relatedText))
            .toList();
    }

    private boolean isRelatedSqlFragment(SqlFragment fragment, Set<String> relatedText) {
        String mapperNamespace = fragment.mapperNamespace().toLowerCase(Locale.ROOT);
        String mapperMethod = fragment.mapperMethod().toLowerCase(Locale.ROOT);
        String mapperClass = mapperNamespace.substring(mapperNamespace.lastIndexOf('.') + 1);
        return relatedText.stream().anyMatch(value -> value.contains(mapperMethod)
            || value.contains(mapperClass.toLowerCase(Locale.ROOT)));
    }

    private List<String> tableNames(List<SqlFragment> sqlFragments) {
        return sqlFragments.stream()
            .flatMap(fragment -> fragment.tables().stream())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private List<String> authorNames(ApiEndpoint endpoint) {
        return endpoint.authors().stream()
            .map(AuthorContribution::name)
            .filter(name -> !name.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    private List<String> authorFilters(ScanResult result) {
        return result.endpoints().stream()
            .flatMap(endpoint -> authorNames(endpoint).stream())
            .distinct()
            .sorted()
            .toList();
    }
}
