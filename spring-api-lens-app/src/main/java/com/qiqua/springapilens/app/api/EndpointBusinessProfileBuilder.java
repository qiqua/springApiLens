package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.AuthorContribution;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.util.List;
import java.util.Locale;

class EndpointBusinessProfileBuilder {
    EndpointDetailResponse.BusinessProfileView build(
        ApiEndpoint endpoint,
        List<CallEdge> callEdges,
        List<SqlFragment> sqlFragments
    ) {
        return new EndpointDetailResponse.BusinessProfileView(
            purpose(endpoint),
            callGuide(endpoint),
            businessFlow(callEdges),
            dataTables(sqlFragments),
            authorSummary(endpoint.authors()),
            risks(sqlFragments),
            testSuggestions(endpoint, sqlFragments)
        );
    }

    private String purpose(ApiEndpoint endpoint) {
        return "处理 %s %s，对应 %s#%s。".formatted(
            endpoint.httpMethod(),
            endpoint.path(),
            endpoint.className(),
            endpoint.methodName()
        );
    }

    private String callGuide(ApiEndpoint endpoint) {
        return "使用 %s %s 调用，请求体 %s，响应 %s。".formatted(
            endpoint.httpMethod(),
            endpoint.path(),
            blankAsDash(endpoint.requestBodyType()),
            blankAsDash(endpoint.responseType())
        );
    }

    private List<String> businessFlow(List<CallEdge> callEdges) {
        if (callEdges.isEmpty()) {
            return List.of("暂未识别到确定性的下游调用链。");
        }
        return callEdges.stream()
            .map(edge -> "%s -> %s：%s".formatted(
                edge.fromSignature(),
                edge.toSignature(),
                blankAsDash(edge.evidence())
            ))
            .toList();
    }

    private List<String> dataTables(List<SqlFragment> sqlFragments) {
        if (sqlFragments.isEmpty()) {
            return List.of("暂未识别到相关 SQL 或数据表。");
        }
        return sqlFragments.stream()
            .flatMap(fragment -> fragment.tables().stream()
                .map(table -> "%s（%s，%s.%s）".formatted(
                    table,
                    blankAsDash(fragment.operationType()),
                    mapperShortName(fragment.mapperNamespace()),
                    fragment.mapperMethod()
                )))
            .distinct()
            .toList();
    }

    private List<String> authorSummary(List<AuthorContribution> authors) {
        if (authors.isEmpty()) {
            return List.of("暂未识别到入口、调用链及相关文件历史的 Git 作者证据。");
        }
        return authors.stream()
            .map(author -> "%s 在入口、调用链及相关文件历史贡献 %d 条证据，约 %d%%。".formatted(
                blankAsDash(author.name()),
                author.lineCount(),
                Math.round(author.ratio() * 100)
            ))
            .toList();
    }

    private List<String> risks(List<SqlFragment> sqlFragments) {
        if (sqlFragments.stream().anyMatch(this::isWriteOperation)) {
            return List.of("该接口包含写操作 insert，建议重点关注幂等性、事务边界和参数校验。");
        }
        if (!sqlFragments.isEmpty()) {
            return List.of("该接口包含数据库读取逻辑，建议关注查询条件、分页和权限过滤。");
        }
        return List.of("当前证据不足，建议人工确认是否存在间接数据库访问或外部系统调用。");
    }

    private boolean isWriteOperation(SqlFragment fragment) {
        String operation = fragment.operationType().toLowerCase(Locale.ROOT);
        return "insert".equals(operation) || "update".equals(operation) || "delete".equals(operation);
    }

    private List<String> testSuggestions(ApiEndpoint endpoint, List<SqlFragment> sqlFragments) {
        String first = "验证 %s %s 的正常请求、参数缺失和异常分支。".formatted(endpoint.httpMethod(), endpoint.path());
        if (sqlFragments.stream().anyMatch(this::isWriteOperation)) {
            return List.of(first, "补充数据库写入后的状态断言，并覆盖重复提交场景。");
        }
        return List.of(first, "补充无数据、权限不足和查询条件边界场景。");
    }

    private String mapperShortName(String mapperNamespace) {
        int dot = mapperNamespace.lastIndexOf('.');
        if (dot >= 0 && dot < mapperNamespace.length() - 1) {
            return mapperNamespace.substring(dot + 1);
        }
        return mapperNamespace;
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
