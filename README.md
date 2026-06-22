# springApiLens

`springApiLens` is a local interface overview tool for Java/Spring Boot projects.

The first MVP scans one local Git repository, extracts Spring endpoints, deterministic Java call edges, MyBatis SQL fragments, and basic Git ownership evidence.

## Build

```powershell
mvn test
```

## Run

```powershell
mvn -pl spring-api-lens-app -am org.springframework.boot:spring-boot-maven-plugin:3.4.2:run
```

## Scan A Repository

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/scan -ContentType 'application/json' -Body '{"repoPath":"D:\\workspace\\demo-springboot","snapshotPath":"D:\\workspace\\spring-api-lens.tsv"}'
```

## List Endpoints

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/endpoints
```

## Current MVP Notes

- Java source scanning currently uses lightweight JDK-based parsing so this repository builds in the available Maven environment.
- Endpoint snapshot persistence currently writes a TSV file through `ScanResultRepository`.
- JavaParser and SQLite remain planned replacement points when those dependencies are available in the build environment.

## Design

- [Interface Overview Tool Design](docs/superpowers/specs/2026-06-22-interface-overview-tool-design.md)
- [Phase One MVP Plan](docs/superpowers/plans/2026-06-22-phase-one-mvp.md)

