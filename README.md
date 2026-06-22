# springApiLens

`springApiLens` is a local interface overview tool for Java/Spring Boot projects.

The planned product scans one local Git repository, builds a Spring-specific code knowledge graph, and provides a Web UI for browsing APIs by Git author, call chain, business logic, related SQL, and database tables.

## First Goal

- Scan a local Spring Boot Git repository.
- Detect Controller endpoints.
- Build Controller -> Service -> Mapper call chains.
- Parse MyBatis SQL and related tables.
- Attribute endpoint-related code to Git authors.
- Use AI to summarize endpoint behavior from scanner evidence.

See the initial design document:

- [Interface Overview Tool Design](docs/superpowers/specs/2026-06-22-interface-overview-tool-design.md)

