# Phase Two UI MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local browser workbench that scans one Spring repository, lists endpoints, filters them, and shows endpoint evidence details.

**Architecture:** Keep Spring Boot as the single local app. Store the latest full `ScanResult` in memory, expose UI-oriented JSON APIs, and serve a dependency-free static HTML/CSS/JavaScript workbench from `spring-api-lens-app/src/main/resources/static`.

**Tech Stack:** Java 21, Spring Boot 3.4.2, JUnit 5, Spring MockMvc, static HTML/CSS/JavaScript.

---

## File Structure

- Modify `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`: keep latest full scan result, expose scan, workbench, and detail endpoints.
- Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/LatestScanStore.java`: owns the latest in-memory `ScanResult`.
- Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointKey.java`: builds and matches stable endpoint keys.
- Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/WorkbenchResponse.java`: main UI payload records.
- Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointDetailResponse.java`: detail payload records.
- Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ApiErrorResponse.java`: safe error payload.
- Modify `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`: add API behavior tests.
- Create `spring-api-lens-app/src/main/resources/static/index.html`: workbench shell.
- Create `spring-api-lens-app/src/main/resources/static/styles.css`: dense engineering UI styles.
- Create `spring-api-lens-app/src/main/resources/static/app.js`: scan, filter, list, and detail behavior.
- Modify `docs/mvp-api.md`: document phase-two API additions.
- Modify `README.md`: document how to open the workbench.

---

### Task 1: Add Stable Endpoint Keys

**Files:**
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointKey.java`
- Test: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`

- [ ] **Step 1: Create endpoint key helper**

Create `EndpointKey.java`:

```java
package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class EndpointKey {
    private EndpointKey() {
    }

    static String from(ApiEndpoint endpoint) {
        String raw = endpoint.className() + "#" + endpoint.methodName() + "|"
            + endpoint.httpMethod() + "|" + endpoint.path();
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static boolean matches(ApiEndpoint endpoint, String key) {
        return from(endpoint).equals(key);
    }
}
```

- [ ] **Step 2: Run current app tests**

Run:

```powershell
mvn -pl spring-api-lens-app -am test
```

Expected: build succeeds with existing tests.

- [ ] **Step 3: Commit**

```powershell
git add -- spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointKey.java
git commit -m "feat: add endpoint keys"
```

---

### Task 2: Store Latest Full Scan Result

**Files:**
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/LatestScanStore.java`
- Modify: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`
- Test: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`

- [ ] **Step 1: Create latest scan store**

Create `LatestScanStore.java`:

```java
package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ScanResult;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LatestScanStore {
    private final AtomicReference<ScanResult> latest = new AtomicReference<>();

    public void save(ScanResult scanResult) {
        latest.set(scanResult);
    }

    public Optional<ScanResult> latest() {
        return Optional.ofNullable(latest.get());
    }
}
```

- [ ] **Step 2: Inject store into controller**

Modify `ScanController.java` so it has:

```java
private final RepositoryScanner repositoryScanner;
private final LatestScanStore latestScanStore;

public ScanController(RepositoryScanner repositoryScanner, LatestScanStore latestScanStore) {
    this.repositoryScanner = repositoryScanner;
    this.latestScanStore = latestScanStore;
}
```

In `scan`, replace `new RepositoryScanner().scan(...)` with `repositoryScanner.scan(...)`, and after optional snapshot save call:

```java
latestScanStore.save(result);
```

Remove the `inMemoryEndpoints` field and make `endpoints()` read from `latestScanStore.latest()`.

- [ ] **Step 3: Expose RepositoryScanner as a bean**

Create a bean method in `SpringApiLensApplication.java`:

```java
@Bean
RepositoryScanner repositoryScanner() {
    return new RepositoryScanner();
}
```

Add imports:

```java
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import org.springframework.context.annotation.Bean;
```

- [ ] **Step 4: Update existing controller test if constructor injection breaks mocks**

In `ScanControllerTest`, add:

```java
@MockBean
private RepositoryScanner repositoryScanner;
```

Configure the mock scan result in tests that call `POST /api/scan`.

- [ ] **Step 5: Run app tests**

Run:

```powershell
mvn -pl spring-api-lens-app -am test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```powershell
git add -- spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/SpringApiLensApplication.java spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/LatestScanStore.java spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java
git commit -m "feat: store latest scan result"
```

---

### Task 3: Add Workbench API Payload

**Files:**
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/WorkbenchResponse.java`
- Modify: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`
- Test: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`

- [ ] **Step 1: Create workbench response records**

Create `WorkbenchResponse.java`:

```java
package com.qiqua.springapilens.app.api;

import java.util.List;

public record WorkbenchResponse(
    RepositoryView repository,
    SummaryView summary,
    List<EndpointListItem> endpoints,
    FilterView filters
) {
    public record RepositoryView(
        String repoName,
        String rootPath,
        String branchName,
        String headCommit,
        boolean hasUncommittedChanges
    ) {
    }

    public record SummaryView(
        int endpointCount,
        int callEdgeCount,
        int sqlFragmentCount,
        int tableCount
    ) {
    }

    public record EndpointListItem(
        String key,
        String httpMethod,
        String path,
        String className,
        String methodName,
        String requestBodyType,
        String responseType,
        String relativeFile,
        int lineStart,
        int lineEnd,
        List<String> tables,
        int callCount
    ) {
    }

    public record FilterView(
        List<String> httpMethods,
        List<String> tables,
        List<String> authors
    ) {
    }
}
```

- [ ] **Step 2: Add `GET /api/workbench`**

In `ScanController.java`, add:

```java
@GetMapping("/workbench")
public WorkbenchResponse workbench() {
    ScanResult result = latestScanStore.latest()
        .orElseGet(() -> new ScanResult(null, List.of(), List.of(), List.of(), List.of()));
    return toWorkbenchResponse(result);
}
```

Add a private `toWorkbenchResponse` method that:

- Returns blank repository values when `repositoryInfo` is null.
- Builds table names from all `SqlFragment.tables()`.
- Builds HTTP methods from endpoints.
- Builds endpoint list items using `EndpointKey.from(endpoint)`.
- Sets `callCount` by counting call edges whose `fromSignature` contains the endpoint class name and method name.
- Sets endpoint item tables conservatively to tables from SQL fragments matched by mapper method evidence; if none match, use `List.of()`.

- [ ] **Step 3: Add test for empty workbench**

In `ScanControllerTest`, add:

```java
@Test
void workbenchReturnsEmptyPayloadBeforeScan() throws Exception {
    mockMvc.perform(get("/api/workbench"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.endpointCount").value(0))
        .andExpect(jsonPath("$.endpoints").isArray());
}
```

- [ ] **Step 4: Add test for populated workbench**

Mock a scan result with one endpoint, one call edge, and one SQL fragment. After `POST /api/scan`, call `GET /api/workbench` and expect:

```java
.andExpect(jsonPath("$.repository.repoName").value("demo"))
.andExpect(jsonPath("$.summary.endpointCount").value(1))
.andExpect(jsonPath("$.summary.tableCount").value(1))
.andExpect(jsonPath("$.endpoints[0].httpMethod").value("POST"))
.andExpect(jsonPath("$.endpoints[0].key").isString())
.andExpect(jsonPath("$.filters.httpMethods[0]").value("POST"))
```

- [ ] **Step 5: Run app tests**

Run:

```powershell
mvn -pl spring-api-lens-app -am test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```powershell
git add -- spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/WorkbenchResponse.java spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java
git commit -m "feat: expose workbench payload"
```

---

### Task 4: Add Endpoint Detail API

**Files:**
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointDetailResponse.java`
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ApiErrorResponse.java`
- Modify: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`
- Test: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`

- [ ] **Step 1: Create detail response records**

Create `EndpointDetailResponse.java`:

```java
package com.qiqua.springapilens.app.api;

import java.util.List;

public record EndpointDetailResponse(
    EndpointView endpoint,
    List<CallEdgeView> callEdges,
    List<SqlFragmentView> sqlFragments,
    List<String> tables,
    List<AuthorView> authors
) {
    public record EndpointView(
        String key,
        String httpMethod,
        String path,
        String className,
        String methodName,
        String requestParamsJson,
        String requestBodyType,
        String responseType,
        String relativeFile,
        int lineStart,
        int lineEnd
    ) {
    }

    public record CallEdgeView(
        String fromSignature,
        String toSignature,
        double confidence,
        String evidence
    ) {
    }

    public record SqlFragmentView(
        String relativeFile,
        String mapperNamespace,
        String mapperMethod,
        String sqlText,
        List<String> tables,
        String operationType
    ) {
    }

    public record AuthorView(
        String name,
        String email,
        double ratio,
        int lineCount
    ) {
    }
}
```

- [ ] **Step 2: Create API error response**

Create `ApiErrorResponse.java`:

```java
package com.qiqua.springapilens.app.api;

public record ApiErrorResponse(String message) {
}
```

- [ ] **Step 3: Add `GET /api/endpoints/{endpointKey}`**

In `ScanController.java`, add:

```java
@GetMapping("/endpoints/{endpointKey}")
public ResponseEntity<?> endpointDetail(@PathVariable String endpointKey) {
    return latestScanStore.latest()
        .flatMap(result -> result.endpoints().stream()
            .filter(endpoint -> EndpointKey.matches(endpoint, endpointKey))
            .findFirst()
            .map(endpoint -> ResponseEntity.ok(toEndpointDetailResponse(result, endpoint))))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiErrorResponse("Endpoint was not found in the latest scan.")));
}
```

Add private mapping methods for endpoint, call edges, SQL fragments, tables, and empty authors.

- [ ] **Step 4: Add detail success test**

After storing a mocked scan result, get the endpoint key from `/api/workbench`, call `/api/endpoints/{key}`, and assert:

```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.endpoint.httpMethod").value("POST"))
.andExpect(jsonPath("$.callEdges[0].evidence").value("service.create(request)"))
.andExpect(jsonPath("$.sqlFragments[0].operationType").value("insert"))
.andExpect(jsonPath("$.tables[0]").value("orders"))
```

- [ ] **Step 5: Add detail 404 test**

```java
mockMvc.perform(get("/api/endpoints/missing"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.message").value("Endpoint was not found in the latest scan."));
```

- [ ] **Step 6: Run app tests**

Run:

```powershell
mvn -pl spring-api-lens-app -am test
```

Expected: tests pass.

- [ ] **Step 7: Commit**

```powershell
git add -- spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointDetailResponse.java spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ApiErrorResponse.java spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java
git commit -m "feat: expose endpoint detail payload"
```

---

### Task 5: Build Static Workbench UI

**Files:**
- Create: `spring-api-lens-app/src/main/resources/static/index.html`
- Create: `spring-api-lens-app/src/main/resources/static/styles.css`
- Create: `spring-api-lens-app/src/main/resources/static/app.js`

- [ ] **Step 1: Create HTML shell**

Create `index.html` with:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>springApiLens</title>
  <link rel="stylesheet" href="/styles.css">
</head>
<body>
  <main class="app-shell">
    <section class="scan-bar" aria-label="Repository scan">
      <div class="brand">
        <strong>springApiLens</strong>
        <span>Local API workbench</span>
      </div>
      <label>
        <span>Repo path</span>
        <input id="repoPathInput" type="text" placeholder="D:\workspace\demo-springboot">
      </label>
      <label>
        <span>Snapshot path</span>
        <input id="snapshotPathInput" type="text" placeholder="Optional TSV path">
      </label>
      <button id="scanButton" type="button">Scan</button>
      <p id="scanStatus" class="status-text">No repository scanned.</p>
    </section>

    <section class="summary-strip" aria-label="Scan summary">
      <div><span>Repository</span><strong id="repoName">-</strong></div>
      <div><span>Endpoints</span><strong id="endpointCount">0</strong></div>
      <div><span>Call edges</span><strong id="callEdgeCount">0</strong></div>
      <div><span>SQL fragments</span><strong id="sqlFragmentCount">0</strong></div>
      <div><span>Tables</span><strong id="tableCount">0</strong></div>
    </section>

    <section class="workspace">
      <aside class="filters-panel">
        <h2>Filters</h2>
        <label>
          <span>Search</span>
          <input id="searchInput" type="search" placeholder="path, class, method, type">
        </label>
        <label>
          <span>HTTP method</span>
          <select id="methodFilter">
            <option value="">All</option>
          </select>
        </label>
        <label>
          <span>Table</span>
          <input id="tableInput" type="search" placeholder="table name">
        </label>
        <label>
          <span>Author</span>
          <input id="authorInput" type="search" placeholder="author keyword">
        </label>
      </aside>

      <section class="endpoint-panel">
        <div class="panel-header">
          <h2>Endpoints</h2>
          <span id="visibleCount">0 shown</span>
        </div>
        <div id="endpointList" class="endpoint-list"></div>
      </section>

      <section class="detail-panel">
        <div class="panel-header">
          <h2>Endpoint Detail</h2>
        </div>
        <div id="detailContent" class="detail-content empty-state">Select an endpoint to inspect evidence.</div>
      </section>
    </section>
  </main>
  <script src="/app.js"></script>
</body>
</html>
```

- [ ] **Step 2: Create CSS**

Create a restrained, dense layout in `styles.css`:

- Body background `#f4f6f8`, text `#17202a`.
- Fixed-height scan bar with compact inputs and button.
- Summary strip as a full-width band, not nested cards.
- Three-column workspace with `260px minmax(360px, 0.9fr) minmax(420px, 1.1fr)`.
- Endpoint rows as single repeated cards with 6px radius.
- Responsive fallback to one column below `900px`.

- [ ] **Step 3: Create JavaScript behavior**

Create `app.js` with functions:

```javascript
let workbench = null;
let selectedKey = null;

async function loadWorkbench() { ... }
async function scanRepository() { ... }
function renderSummary() { ... }
function renderMethodOptions() { ... }
function filteredEndpoints() { ... }
function renderEndpointList() { ... }
async function selectEndpoint(key) { ... }
function renderDetail(detail) { ... }
function escapeHtml(value) { ... }
```

Use `fetch('/api/scan')`, `fetch('/api/workbench')`, and `fetch('/api/endpoints/' + encodeURIComponent(key))`.

- [ ] **Step 4: Run app tests**

Run:

```powershell
mvn -pl spring-api-lens-app -am test
```

Expected: backend tests still pass.

- [ ] **Step 5: Commit**

```powershell
git add -- spring-api-lens-app/src/main/resources/static/index.html spring-api-lens-app/src/main/resources/static/styles.css spring-api-lens-app/src/main/resources/static/app.js
git commit -m "feat: add static workbench UI"
```

---

### Task 6: Document Phase-Two Usage

**Files:**
- Modify: `README.md`
- Modify: `docs/mvp-api.md`

- [ ] **Step 1: Update README**

Add:

```markdown
## Open Workbench

Start the app:

```powershell
mvn -pl spring-api-lens-app -am org.springframework.boot:spring-boot-maven-plugin:3.4.2:run
```

Open:

```text
http://localhost:8080/
```
```

- [ ] **Step 2: Update API docs**

Document:

- `GET /api/workbench`
- `GET /api/endpoints/{endpointKey}`
- Static UI at `/`

- [ ] **Step 3: Run tests**

Run:

```powershell
mvn test
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```powershell
git add -- README.md docs/mvp-api.md
git commit -m "docs: document workbench UI"
```

---

### Task 7: Browser Verification

**Files:**
- No source edits expected unless verification finds defects.

- [ ] **Step 1: Start the Spring Boot app**

Run:

```powershell
mvn -pl spring-api-lens-app -am org.springframework.boot:spring-boot-maven-plugin:3.4.2:run
```

Expected: app starts on `http://localhost:8080`.

- [ ] **Step 2: Open workbench**

Open:

```text
http://localhost:8080/
```

Expected:

- Scan bar is visible.
- Summary strip is visible.
- Filters, endpoint list, and detail panels are visible.
- Text does not overlap at desktop width.

- [ ] **Step 3: Scan this repository as a smoke test**

Use repo path:

```text
C:\Users\Administrator\Desktop\转正申请和ppt\springApiLens\.worktrees\phase-two-ui-mvp
```

Expected:

- Scan completes.
- Endpoint rows render.
- Selecting one endpoint renders detail sections.

- [ ] **Step 4: Stop server**

Stop the Spring Boot process cleanly after verification.

- [ ] **Step 5: Commit any verification fixes**

If edits were needed:

```powershell
git add -- <changed-files>
git commit -m "fix: polish workbench verification issues"
```

---

### Task 8: Final Verification and Push

**Files:**
- No source edits expected.

- [ ] **Step 1: Run full tests**

Run:

```powershell
mvn test
```

Expected: build success, all tests pass.

- [ ] **Step 2: Check clean status**

Run:

```powershell
git status --short --branch
```

Expected: clean worktree on `feature/phase-two-ui-mvp`.

- [ ] **Step 3: Push branch**

Use the known working GitHub network path for this machine:

```powershell
git -c http.sslBackend=openssl -c http.proxy=http://127.0.0.1:7897 -c https.proxy=http://127.0.0.1:7897 push -u origin feature/phase-two-ui-mvp
```

Expected: branch is pushed and tracks `origin/feature/phase-two-ui-mvp`.
