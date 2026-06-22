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

## Implementation Note

The first MVP uses a TSV endpoint snapshot via `ScanResultRepository`. SQLite is still part of the target architecture, but the driver is not available in the current Maven repository, so the persistence boundary is intentionally kept small and replaceable.
