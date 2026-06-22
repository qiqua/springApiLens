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
      "callCount": 3
    }
  ],
  "filters": {
    "httpMethods": ["GET", "POST"],
    "tables": ["order_main"],
    "authors": []
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
  "authors": []
}
```

If the endpoint key is not found, the API returns HTTP 404:

```json
{
  "message": "Endpoint was not found in the latest scan."
}
```

## Static Workbench

The Spring Boot app serves the local workbench at:

```text
http://localhost:8080/
```

## Implementation Note

The first MVP uses a TSV endpoint snapshot via `ScanResultRepository`. SQLite is still part of the target architecture, but the driver is not available in the current Maven repository, so the persistence boundary is intentionally kept small and replaceable.
