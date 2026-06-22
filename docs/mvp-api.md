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
  "content": "作者: Ada\n业务逻辑: ...",
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

## Static Workbench

The Spring Boot app serves the local workbench at:

```text
http://localhost:8080/
```

## Implementation Note

The first MVP uses a TSV endpoint snapshot via `ScanResultRepository`. SQLite is still part of the target architecture, but the driver is not available in the current Maven repository, so the persistence boundary is intentionally kept small and replaceable.
