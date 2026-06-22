# springApiLens

`springApiLens` is a local interface overview tool for Java/Spring Boot projects.

The current MVP scans one local Git repository, extracts Spring endpoints, deterministic Java call edges, MyBatis SQL fragments, Git ownership evidence, and optional AI endpoint summaries.

## Build

```powershell
mvn test
```

## Run

```powershell
mvn -pl spring-api-lens-app -am org.springframework.boot:spring-boot-maven-plugin:3.4.2:run
```

## Open Workbench

After the app starts, open:

```text
http://localhost:8080/
```

The local workbench lets you scan one Git repository, browse endpoints, filter results, and inspect endpoint evidence.

Endpoint detail now includes author ownership from `git blame`. The AI Summary button can answer who wrote the endpoint, what the business logic does, how to call it, and which data/tables are involved.

## Scan A Repository

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/scan -ContentType 'application/json' -Body '{"repoPath":"D:\\workspace\\demo-springboot","snapshotPath":"D:\\workspace\\spring-api-lens.tsv"}'
```

## List Endpoints

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/endpoints
```

## Workbench API

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/workbench
```

Select an endpoint key from the workbench payload, then inspect detail evidence:

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/endpoints/<endpointKey>
```

Generate an optional AI summary for the selected endpoint:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/endpoints/<endpointKey>/ai-summary
```

## AI Configuration

AI is disabled until you add a local config file. Copy the example:

```powershell
Copy-Item .spring-api-lens\ai-config.example.json .spring-api-lens\ai-config.json
```

Set the API key in your shell, not in Git:

```powershell
$env:SPRING_API_LENS_AI_KEY = "your-key"
```

Default config path:

```text
.spring-api-lens/ai-config.json
```

Override the config path:

```powershell
$env:SPRING_API_LENS_AI_CONFIG = "D:\configs\spring-api-lens-ai.json"
```

Config shape:

```json
{
  "enabled": true,
  "provider": "deepseek",
  "baseUrl": "https://api.deepseek.com",
  "model": "deepseek-chat",
  "apiKeyEnv": "SPRING_API_LENS_AI_KEY"
}
```

The client is OpenAI-compatible and posts to `{baseUrl}/v1/chat/completions`, so local Ollama/vLLM/OneAPI-style gateways and hosted providers can be switched by changing `provider`, `baseUrl`, `model`, and `apiKeyEnv`.

## Current MVP Notes

- Java source scanning currently uses lightweight JDK-based parsing so this repository builds in the available Maven environment.
- Endpoint snapshot persistence currently writes a TSV file through `ScanResultRepository`.
- AI calls are opt-in and use a local JSON config plus environment variables so provider/model/key are never hardcoded.
- JavaParser and SQLite remain planned replacement points when those dependencies are available in the build environment.

## Design

- [Interface Overview Tool Design](docs/superpowers/specs/2026-06-22-interface-overview-tool-design.md)
- [Phase One MVP Plan](docs/superpowers/plans/2026-06-22-phase-one-mvp.md)
- [Phase Two UI MVP Design](docs/superpowers/specs/2026-06-22-phase-two-ui-mvp-design.md)
- [Phase Two UI MVP Plan](docs/superpowers/plans/2026-06-22-phase-two-ui-mvp.md)

