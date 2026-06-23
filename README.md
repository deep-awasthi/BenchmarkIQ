# BenchmarkIQ

BenchmarkIQ is a Spring Boot API performance testing and reliability platform. It lets users register or log in, create reusable API load-test configurations, start and stop executions, stream live metrics over WebSocket, and view dashboards and reports.

## Tech Stack

- Java 21
- Spring Boot 3.3.0
- Maven
- Spring Security with JWT authentication
- Spring Data JPA
- H2 in-memory database
- Caffeine cache
- Spring WebSocket
- SpringDoc OpenAPI / Swagger UI
- Docker and Docker Compose

## Project Structure

```text
.
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src
    ├── main
    │   ├── java/com/benchmarkiq
    │   │   ├── config
    │   │   ├── controller
    │   │   ├── dto
    │   │   ├── engine
    │   │   ├── entity
    │   │   ├── exception
    │   │   ├── repository
    │   │   ├── scheduler
    │   │   ├── security
    │   │   ├── service
    │   │   └── websocket
    │   └── resources
    │       ├── application.yml
    │       └── data.sql
    └── test
```

## Prerequisites

Install the following before running the project locally:

- JDK 21
- Maven 3.9 or newer
- Docker Desktop, optional but recommended for container runs
- curl or an API client such as Postman

Check your installed versions:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## Quick Start

Run the app with Maven:

```bash
mvn spring-boot:run
```

The API starts on:

```text
http://localhost:8080/api
```

Useful local URLs:

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- H2 console: `http://localhost:8080/api/h2-console`
- Health check: `http://localhost:8080/api/actuator/health`
- App info: `http://localhost:8080/api/actuator/info`

## Default Users

The app seeds two users from `src/main/resources/data.sql` on startup:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `Admin@123` |
| User | `testuser` | `User@123` |

## Login

Get a JWT access token:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123"
  }'
```

Save the token for protected endpoints:

```bash
export TOKEN="<accessToken-from-login-response>"
```

Then pass it as a Bearer token:

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer $TOKEN"
```

## Run With Docker Compose

Build and start the container:

```bash
docker compose up --build
```

Run it in the background:

```bash
docker compose up --build -d
```

View logs:

```bash
docker compose logs -f benchmarkiq
```

Stop the container:

```bash
docker compose down
```

## Build

Create a runnable jar:

```bash
mvn clean package
```

Run the packaged app:

```bash
java -jar target/benchmarkiq-1.0.0.jar
```

Skip tests during packaging:

```bash
mvn clean package -DskipTests
```

## Test

Run all tests:

```bash
mvn test
```

Run one test class:

```bash
mvn test -Dtest=AuthServiceTest
```

Run one test method:

```bash
mvn test -Dtest=AuthServiceTest#login_WithValidCredentials_ReturnsAuthResponse
```

## Configuration

Main configuration lives in `src/main/resources/application.yml`.

Important defaults:

| Setting | Default |
| --- | --- |
| Server port | `8080` |
| Context path | `/api` |
| Database | H2 in-memory |
| H2 JDBC URL | `jdbc:h2:mem:benchmarkiqdb` |
| H2 username | `sa` |
| H2 password | `password` |
| JWT lifetime | 24 hours |
| Max concurrent users | `1000` |
| Max test duration | `3600` seconds |

Docker Compose sets these environment variables:

```text
SPRING_PROFILES_ACTIVE=default
APP_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
APP_JWT_EXPIRATION_MS=86400000
SERVER_PORT=8080
```

For production, replace the default JWT secret with a strong private secret.

## H2 Console

Open:

```text
http://localhost:8080/api/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:benchmarkiqdb
User Name: sa
Password: password
```

## API Documentation

Open Swagger UI after the app starts:

```text
http://localhost:8080/api/swagger-ui.html
```

For a curl-based API walkthrough, see [docs/API.md](docs/API.md).

## WebSocket Metrics

Live metrics are available at:

```text
ws://localhost:8080/api/ws/metrics/{executionId}
```

Connect after starting a test execution. The socket sends metric updates for the matching execution ID and closes after completion.

## Common Workflow

1. Start the app with `mvn spring-boot:run` or `docker compose up --build`.
2. Log in with one of the seeded users.
3. Save the returned `accessToken` as `TOKEN`.
4. Create a test configuration.
5. Start a test execution with the configuration ID.
6. Watch live metrics over WebSocket or poll execution details.
7. Stop the execution if needed.
8. Fetch the report after completion.

## Troubleshooting

If port `8080` is already in use, stop the other process or change `server.port` in `application.yml`.

If authentication fails, confirm that the app restarted cleanly and that `data.sql` seeded the default users. The in-memory database resets every time the app restarts.

If Docker health checks fail, inspect logs:

```bash
docker compose logs benchmarkiq
```

If tests fail because of a stale build, clean and retry:

```bash
mvn clean test
```
