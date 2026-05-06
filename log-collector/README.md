# log-collector — E-Commerce Event Log Collector

A lightweight **Spring Boot 3** microservice that exposes a REST API to ingest and validate e-commerce user-event payloads.  
Packaged as an **Uber JAR** (via `maven-shade-plugin`) and automatically built into a **Docker image** (`log-collector:latest`) on every `mvn clean install`.

---

## Project Structure

```
log-collector/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/devops/logcollector/
    │   │   ├── LogCollectorApplication.java      ← Spring Boot entry-point
    │   │   ├── controller/
    │   │   │   ├── LogController.java             ← POST /api/logs
    │   │   │   └── GlobalExceptionHandler.java   ← 400 / 500 error handling
    │   │   └── model/
    │   │       └── UserEvent.java                 ← Request payload + validation
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/devops/logcollector/controller/
            └── LogControllerTest.java             ← MockMvc slice tests
```

---

## API

### `POST /api/logs`

| Field       | Type   | Required | Description                            |
|-------------|--------|----------|----------------------------------------|
| `userId`    | String | ✅        | Unique identifier of the user          |
| `action`    | String | ✅        | Event type (e.g. `ADD_TO_CART`)        |
| `timestamp` | String | ✅        | ISO-8601 UTC, e.g. `2024-06-01T10:15:30Z` |
| `productId` | String | ❌        | Product involved (optional)            |

**Example request:**
```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "userId":    "user-42",
    "action":    "ADD_TO_CART",
    "timestamp": "2024-06-01T10:15:30Z",
    "productId": "prod-789"
  }'
```

**202 Accepted response:**
```json
{
  "status":     "accepted",
  "message":    "Event received and logged successfully",
  "userId":     "user-42",
  "action":     "ADD_TO_CART",
  "receivedAt": "2024-06-01T10:15:32.123Z"
}
```

**400 Bad Request (validation failure):**
```json
{
  "status":  400,
  "error":   "Bad Request",
  "message": "Validation failed",
  "details": ["userId must not be blank"],
  "timestamp": "2024-06-01T10:15:30Z"
}
```

---

## Build & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for image build)

### 1 — Build Uber JAR only
```bash
mvn clean package
# Output: target/log-collector.jar
```

### 2 — Build Uber JAR + Docker image (`log-collector:latest`)
```bash
mvn clean install
# Docker daemon must be running!
```

### 3 — Run locally (JVM)
```bash
java -jar target/log-collector.jar
```

### 4 — Run via Docker
```bash
docker run -p 8080:8080 log-collector:latest
```

### 5 — Run tests
```bash
mvn test
```

---

## Key Design Decisions

| Concern | Decision |
|---|---|
| Packaging | `maven-shade-plugin` creates a single fat JAR; Spring Boot repackage is disabled to avoid conflicts |
| Docker build | Spotify `dockerfile-maven-plugin` hooks into the `install` phase |
| Base image | `eclipse-temurin:17-jre-alpine` (~90 MB, no JDK overhead) |
| Security | Non-root user (`appuser`) inside the container |
| JVM tuning | `UseContainerSupport` + `MaxRAMPercentage=75` for safe heap sizing in K8s/Docker |
| Validation | Jakarta Bean Validation (`@NotBlank`, `@NotNull`) on the request model |
| Error handling | `@RestControllerAdvice` returns structured JSON for all 4xx/5xx errors |
