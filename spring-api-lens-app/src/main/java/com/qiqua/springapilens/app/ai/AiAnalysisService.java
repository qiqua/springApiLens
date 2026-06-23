package com.qiqua.springapilens.app.ai;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AiAnalysisService {
    private final Supplier<AiConfig> configSupplier;
    private final AiClient aiClient;

    public AiAnalysisService(Supplier<AiConfig> configSupplier, AiClient aiClient) {
        this.configSupplier = configSupplier;
        this.aiClient = aiClient;
    }

    public AiSummary analyze(
        ScanResult scanResult,
        ApiEndpoint endpoint,
        List<CallEdge> callEdges,
        List<SqlFragment> sqlFragments,
        List<String> tables
    ) {
        AiConfig config = configSupplier.get();
        if (!config.configured()) {
            return AiSummary.disabled(config);
        }

        String content = aiClient.complete(new AiAnalysisRequest(config, buildPrompt(scanResult, endpoint, callEdges, sqlFragments, tables)));
        return new AiSummary(true, true, config.provider(), config.model(), content, "");
    }

    private String buildPrompt(
        ScanResult scanResult,
        ApiEndpoint endpoint,
        List<CallEdge> callEdges,
        List<SqlFragment> sqlFragments,
        List<String> tables
    ) {
        return """
            你是一个 Java/Spring 接口分析助手。只能基于下面的证据回答，不要编造代码中没有的事实。
            请用中文输出，并严格使用这些小节：
            1. 接口用途
            2. 调用方式
            3. 业务逻辑
            4. 代码调用链
            5. 数据和表
            6. 作者归属
            7. 风险点
            8. 测试建议
            9. 不确定点

            仓库: %s
            分支: %s
            提交: %s

            接口: %s %s
            控制器: %s#%s
            源码: %s:%d-%d
            请求参数: %s
            请求体: %s
            响应: %s

            作者归因:
            %s

            调用链证据:
            %s

            SQL 和表:
            %s

            表列表: %s
            """.formatted(
            scanResult.repositoryInfo().repoName(),
            scanResult.repositoryInfo().currentBranch(),
            scanResult.repositoryInfo().headCommit(),
            endpoint.httpMethod(),
            endpoint.path(),
            endpoint.className(),
            endpoint.methodName(),
            endpoint.relativeFile(),
            endpoint.lineStart(),
            endpoint.lineEnd(),
            endpoint.requestParamsJson(),
            blankAsDash(endpoint.requestBodyType()),
            blankAsDash(endpoint.responseType()),
            authorEvidence(endpoint.authors()),
            callEvidence(callEdges),
            sqlEvidence(sqlFragments),
            tables.isEmpty() ? "-" : String.join(", ", tables)
        );
    }

    private String authorEvidence(List<AuthorContribution> authors) {
        if (authors.isEmpty()) {
            return "-";
        }
        return authors.stream()
            .map(author -> "- %s <%s>, %.2f, %d evidence items".formatted(
                author.name(),
                author.email(),
                author.ratio(),
                author.lineCount()
            ))
            .collect(Collectors.joining("\n"));
    }

    private String callEvidence(List<CallEdge> callEdges) {
        if (callEdges.isEmpty()) {
            return "-";
        }
        return callEdges.stream()
            .map(edge -> "- %s -> %s, confidence %.2f, evidence: %s".formatted(
                edge.fromSignature(),
                edge.toSignature(),
                edge.confidence(),
                edge.evidence()
            ))
            .collect(Collectors.joining("\n"));
    }

    private String sqlEvidence(List<SqlFragment> sqlFragments) {
        if (sqlFragments.isEmpty()) {
            return "-";
        }
        return sqlFragments.stream()
            .map(fragment -> "- %s.%s [%s] tables=%s sql=%s".formatted(
                fragment.mapperNamespace(),
                fragment.mapperMethod(),
                fragment.operationType(),
                String.join(", ", fragment.tables()),
                fragment.sqlText()
            ))
            .collect(Collectors.joining("\n"));
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
