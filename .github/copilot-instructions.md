# Copilot Instructions for avalanche-reports

## Project Overview

Azure Functions (Java 17, serverless) app that fetches avalanche bulletins from Poland (TOPR via web scraping with HTMLUnit) and Slovakia (EAWS XML feed), stores them in MongoDB, and serves them via HTTP endpoints. No Spring — uses Azure Functions native DI via constructor injection with factory `Function<>` providers.

## Build & Test Commands

```bash
# Build
mvn clean package

# Run locally (Azure Functions CLI required)
mvn azure-functions:run        # http://localhost:7071

# Tests
mvn test                       # Unit tests only (*Test.java)
mvn verify                     # Unit + integration tests (*IT.java, requires Docker)

# Single test class or method
mvn test -Dtest=AvalancheReportMapperTest
mvn test -Dtest=AvalancheReportMapperTest#someMethod

# Deploy to Azure
mvn azure-functions:deploy -Dapp.name=... -Dapp.plan.name=... -Dapp.resource.group=...
```

## Local Development

Start MongoDB with Docker Compose before running integration tests or the function locally:

```bash
docker-compose up -d   # MongoDB on :27017, Mongo Express UI on :8081 (credentials: mongo/mongo)
```

Local MongoDB connection string: `mongodb://mongo:mongo@localhost/avalanches_local:27017?ssl=false&authSource=admin`

Production uses the `CosmosDBConnectionString` environment variable.

## Architecture

```
HTTP/Timer Trigger
    └── AvalancheReportsFunctionsHandler  (function/)
            ├── AvalancheReportClient     (client/)  – TOPR (HTMLUnit scrape) or Slovakia (XML)
            ├── AvalancheReportRepository (mongo/)   – MongoDB via direct driver (no ORM)
            └── AvalancheReportsMapper    (function/) – MapStruct domain→DTO
```

**Package layout under `src/main/java/walaniam/avalanches/`:**

| Package | Role |
|---|---|
| `function/` | `@FunctionName` handler, MapStruct mapper, DTOs |
| `client/` | `AvalancheReportClient` interface + TOPR & Slovakia implementations |
| `persistence/` | Domain models (`AvalancheReport`, `DataRange`) |
| `mongo/` | Repository implementations, `MongoClientExecutor` wrapper |
| `regions/` | Region/area mapping helpers, `regions_pl.json` |
| `common/` | Logging and DateTime utilities |

## Azure Functions

| Function | Trigger | Auth | Notes |
|---|---|---|---|
| `ingestReport` | Timer – daily 18:30 UTC | — | Fetch & persist reports |
| `ingestReportOnDemand` | HTTP GET | FUNCTION | Manual ingest trigger |
| `reports` | HTTP GET | ANONYMOUS | Paginated list (`page`, `size` params) |
| `report` | HTTP GET | ANONYMOUS | Latest single report |
| `avalanche` | HTTP GET | ANONYMOUS | Serve `avalanche_template.html` |
| `pdfReport` | HTTP GET `/pdfs/{day}` | ANONYMOUS | PDF by date |

## Key Conventions

### Naming
- Test files: `*Test.java` (unit), `*IT.java` (integration with TestContainers)
- DTOs: `*Dto` suffix
- Mongo implementations: `*MongoRepository` suffix
- MapStruct mappers: `*Mapper` suffix with a static `INSTANCE` field

### Domain Models
Use Lombok `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` on domain classes.

### Dependency Injection
No Spring. Dependencies are passed as constructor arguments; repositories are provided via `Function<ExecutionContext, AvalancheReportRepository>` factory lambdas for lazy initialisation per invocation.

```java
@RequiredArgsConstructor
public class AvalancheReportsFunctionsHandler {
    private final Function<ExecutionContext, AvalancheReportRepository> repositoryProvider;
    // usage:
    AvalancheReportRepository repo = repositoryProvider.apply(context);
}
```

### Mappers
MapStruct interfaces with a static `INSTANCE`:

```java
@Mapper
public interface AvalancheReportsMapper {
    AvalancheReportsMapper INSTANCE = Mappers.getMapper(AvalancheReportsMapper.class);
    AvalancheReportDto toDto(AvalancheReport report);
}
```

### Testing
- Unit tests: JUnit 5 + Mockito + AssertJ + WireMock
- Integration tests: JUnit 5 + TestContainers (real MongoDB container) — requires Docker
- Integration test base classes set up the container once per class (`@Testcontainers`, `@Container` static field)
