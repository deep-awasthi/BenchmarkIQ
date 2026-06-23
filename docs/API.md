# BenchmarkIQ API Guide

This guide shows the main API flow with curl. Start the app before running these commands.

```bash
mvn spring-boot:run
```

Set the base URL:

```bash
export BASE_URL="http://localhost:8080/api"
```

## Authentication

Register a new user:

```bash
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo_user",
    "email": "demo@example.com",
    "password": "Demo@1234"
  }'
```

Log in with the seeded admin user:

```bash
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123"
  }'
```

Save the returned token:

```bash
export TOKEN="<accessToken>"
```

Every protected endpoint needs:

```text
Authorization: Bearer <accessToken>
```

## Test Configurations

Create a test configuration:

```bash
curl -s -X POST "$BASE_URL/test-configs" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Example GET test",
    "description": "Small smoke load test against httpbin",
    "targetUrl": "https://httpbin.org/get",
    "httpMethod": "GET",
    "headers": {
      "Accept": "application/json"
    },
    "concurrentUsers": 5,
    "durationSeconds": 30,
    "rampUpSeconds": 5,
    "maxAverageLatencyMs": 1000,
    "maxP95LatencyMs": 2000,
    "maxErrorRatePercent": 5.0
  }'
```

List configurations:

```bash
curl -s "$BASE_URL/test-configs?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Search configurations by name:

```bash
curl -s "$BASE_URL/test-configs?name=Example&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Get one configuration:

```bash
curl -s "$BASE_URL/test-configs/1" \
  -H "Authorization: Bearer $TOKEN"
```

Update a configuration:

```bash
curl -s -X PUT "$BASE_URL/test-configs/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Example GET test updated",
    "description": "Updated smoke load test",
    "targetUrl": "https://httpbin.org/get",
    "httpMethod": "GET",
    "headers": {
      "Accept": "application/json"
    },
    "concurrentUsers": 10,
    "durationSeconds": 45,
    "rampUpSeconds": 5,
    "maxAverageLatencyMs": 1000,
    "maxP95LatencyMs": 2000,
    "maxErrorRatePercent": 5.0
  }'
```

Delete a configuration:

```bash
curl -s -X DELETE "$BASE_URL/test-configs/1" \
  -H "Authorization: Bearer $TOKEN"
```

Supported HTTP methods for configurations:

```text
GET, POST, PUT, DELETE, PATCH
```

Limits enforced by validation:

| Field | Limit |
| --- | --- |
| `concurrentUsers` | 1 to 1000 |
| `durationSeconds` | 1 to 3600 |
| `rampUpSeconds` | 0 to 300 |
| `maxErrorRatePercent` | 0.0 to 100.0 |

## Test Executions

Start a test from a configuration:

```bash
curl -s -X POST "$BASE_URL/executions/start/1" \
  -H "Authorization: Bearer $TOKEN"
```

Save the returned execution ID:

```bash
export EXECUTION_ID="<executionId>"
```

Stop a running test:

```bash
curl -s -X POST "$BASE_URL/executions/stop/$EXECUTION_ID" \
  -H "Authorization: Bearer $TOKEN"
```

List currently running tests:

```bash
curl -s "$BASE_URL/executions/running" \
  -H "Authorization: Bearer $TOKEN"
```

Get execution details:

```bash
curl -s "$BASE_URL/executions/$EXECUTION_ID" \
  -H "Authorization: Bearer $TOKEN"
```

Get execution history for a configuration:

```bash
curl -s "$BASE_URL/executions/history/1?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## Live Metrics WebSocket

Connect to live metrics for one execution:

```text
ws://localhost:8080/api/ws/metrics/{executionId}
```

Example with `wscat`:

```bash
npx wscat -c "ws://localhost:8080/api/ws/metrics/$EXECUTION_ID"
```

Example metric payload:

```json
{
  "executionId": 1,
  "totalRequests": 120,
  "successfulRequests": 118,
  "failedRequests": 2,
  "requestsPerSecond": 9.8,
  "errorRatePercent": 1.67,
  "averageLatencyMs": 145,
  "currentConcurrentUsers": 5,
  "elapsedSeconds": 12
}
```

Completion payload:

```json
{
  "type": "COMPLETED",
  "executionId": 1
}
```

## Dashboard

Get dashboard summary:

```bash
curl -s "$BASE_URL/dashboard/summary" \
  -H "Authorization: Bearer $TOKEN"
```

Get latest results:

```bash
curl -s "$BASE_URL/dashboard/latest?limit=10" \
  -H "Authorization: Bearer $TOKEN"
```

Get trend data:

```bash
curl -s "$BASE_URL/dashboard/trend/1?days=7" \
  -H "Authorization: Bearer $TOKEN"
```

## Reports

Generate a report for a completed execution:

```bash
curl -s "$BASE_URL/reports/$EXECUTION_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## System Endpoints

Health:

```bash
curl -s "$BASE_URL/actuator/health"
```

Info:

```bash
curl -s "$BASE_URL/actuator/info"
```

Swagger UI:

```text
http://localhost:8080/api/swagger-ui.html
```

H2 console:

```text
http://localhost:8080/api/h2-console
```
