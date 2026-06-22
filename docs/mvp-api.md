# MVP API

## POST /api/scan

Runs a local repository scan.

Request:

```json
{
  "repoPath": "D:\\workspace\\demo-springboot",
  "snapshotPath": "D:\\workspace\\spring-api-lens.tsv"
}
```

Response:

```json
{
  "repoName": "demo-springboot",
  "endpointCount": 4,
  "callEdgeCount": 12,
  "sqlFragmentCount": 8
}
```

## GET /api/endpoints

Returns endpoints from the latest in-memory scan.

Response:

```json
[
  {
    "httpMethod": "POST",
    "path": "/api/order/create",
    "className": "OrderController",
    "methodName": "create",
    "requestBodyType": "CreateOrderRequest",
    "responseType": "ApiResult<OrderVO>"
  }
]
```

## GET /api/workbench

Returns the latest scan in a UI-friendly shape.

Response:

```json
{
  "repository": {
    "repoName": "demo-springboot",
    "rootPath": "D:\\workspace\\demo-springboot",
    "branchName": "main",
    "headCommit": "abc123",
    "hasUncommittedChanges": false
  },
  "summary": {
    "endpointCount": 4,
    "callEdgeCount": 12,
    "sqlFragmentCount": 8,
    "tableCount": 5
  },
  "endpoints": [
    {
      "key": "encoded-endpoint-key",
      "httpMethod": "POST",
      "path": "/api/order/create",
      "className": "OrderController",
      "methodName": "create",
      "requestBodyType": "CreateOrderRequest",
      "responseType": "ApiResult<OrderVO>",
      "relativeFile": "src/main/java/com/demo/OrderController.java",
      "lineStart": 42,
      "lineEnd": 86,
      "tables": ["order_main"],
      "callCount": 3,
      "authors": ["Ada"]
    }
  ],
  "filters": {
    "httpMethods": ["GET", "POST"],
    "tables": ["order_main"],
    "authors": ["Ada"]
  }
}
```

## GET /api/endpoints/{endpointKey}

Returns detail evidence for one endpoint from the latest in-memory scan.

Response:

```json
{
  "endpoint": {
    "key": "encoded-endpoint-key",
    "httpMethod": "POST",
    "path": "/api/order/create",
    "className": "OrderController",
    "methodName": "create",
    "requestParamsJson": "[]",
    "requestBodyType": "CreateOrderRequest",
    "responseType": "ApiResult<OrderVO>",
    "relativeFile": "src/main/java/com/demo/OrderController.java",
    "lineStart": 42,
    "lineEnd": 86
  },
  "profile": {
    "purpose": "处理 POST /api/order/create，对应 OrderController#create。",
    "callGuide": "使用 POST /api/order/create 调用，请求体 CreateOrderRequest，响应 ApiResult<OrderVO>。",
    "businessFlow": [
      "OrderController.create() -> OrderService.create()：orderService.create"
    ],
    "dataTables": [
      "order_main（insert，OrderMapper.insertOrder）"
    ],
    "authorSummary": [
      "Ada 贡献 18 行，约 75%。"
    ],
    "risks": [
      "该接口包含写操作 insert，建议重点关注幂等性、事务边界和参数校验。"
    ],
    "testSuggestions": [
      "验证 POST /api/order/create 的正常请求、参数缺失和异常分支。"
    ]
  },
  "callEdges": [
    {
      "fromSignature": "OrderController.create()",
      "toSignature": "OrderService.create()",
      "confidence": 0.95,
      "evidence": "orderService.create"
    }
  ],
  "sqlFragments": [
    {
      "relativeFile": "src/main/resources/mapper/OrderMapper.xml",
      "mapperNamespace": "com.demo.OrderMapper",
      "mapperMethod": "insertOrder",
      "sqlText": "insert into order_main ...",
      "tables": ["order_main"],
      "operationType": "insert"
    }
  ],
  "tables": ["order_main"],
  "authors": [
    {
      "name": "Ada",
      "email": "ada@example.com",
      "ratio": 0.75,
      "lineCount": 18
    }
  ]
}
```

If the endpoint key is not found, the API returns HTTP 404:

```json
{
  "message": "Endpoint was not found in the latest scan."
}
```

## POST /api/endpoints/{endpointKey}/ai-summary

Generates an optional OpenAI-compatible AI summary for one endpoint from the latest in-memory scan.

If AI is configured:

```json
{
  "enabled": true,
  "configured": true,
  "provider": "deepseek",
  "model": "deepseek-chat",
  "content": "Author: Ada\nBusiness logic: ...",
  "message": ""
}
```

If AI is not configured:

```json
{
  "enabled": false,
  "configured": false,
  "provider": "",
  "model": "",
  "content": "",
  "message": "AI is disabled. Configure .spring-api-lens/ai-config.json to enable summaries."
}
```

AI config is loaded from `.spring-api-lens/ai-config.json` by default, or from `SPRING_API_LENS_AI_CONFIG` when that environment variable is set. API keys are read from the environment variable named by `apiKeyEnv`.

## GET /api/history

Returns previous scans saved under `.spring-api-lens/history/`.

Response:

```json
[
  {
    "id": "20260622090000-abcd1234",
    "scannedAt": "2026-06-22T09:00:00Z",
    "repoName": "demo-springboot",
    "rootPath": "D:\\workspace\\demo-springboot",
    "branchName": "main",
    "headCommit": "abc123",
    "endpointCount": 4,
    "callEdgeCount": 12,
    "sqlFragmentCount": 8
  }
]
```

## POST /api/history/{scanId}/load

Loads one previous scan into the in-memory workbench state.

Response:

```json
{
  "repoName": "demo-springboot",
  "endpointCount": 4,
  "callEdgeCount": 12,
  "sqlFragmentCount": 8
}
```

## GET /api/ai-config

Returns the current AI configuration status without exposing the raw API key.

Response:

```json
{
  "enabled": true,
  "configured": false,
  "provider": "deepseek",
  "baseUrl": "https://api.deepseek.com",
  "model": "deepseek-chat",
  "apiKeyEnv": "SPRING_API_LENS_AI_KEY",
  "message": "AI is enabled but baseUrl, model, or API key is missing."
}
```

## POST /api/ai-config

Saves AI provider settings to the local AI config file. The raw key is not part of the request; only the environment variable name is stored.

Request:

```json
{
  "enabled": true,
  "provider": "local",
  "baseUrl": "http://127.0.0.1:11434",
  "model": "qwen",
  "apiKeyEnv": "LOCAL_AI_KEY"
}
```

## Static Workbench

The Spring Boot app serves the local workbench at:

```text
http://localhost:8080/
```

The workbench UI uses Chinese user-facing text. Endpoint detail shows the structured business profile before the raw evidence sections.

## Implementation Note

The first MVP uses a TSV endpoint snapshot via `ScanResultRepository`. The workbench scan history currently uses JSON files. SQLite is still part of the target architecture, but the driver is not available in the current Maven repository, so the persistence boundary is intentionally kept small and replaceable.
