package com.benchmarkiq.engine;

import com.benchmarkiq.entity.TestConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class HttpRequestWorker implements Runnable {

    private final TestConfig config;
    private final MetricsCollector metrics;
    private final HttpClient httpClient;
    private final int timeoutMs;

    public HttpRequestWorker(TestConfig config, MetricsCollector metrics, HttpClient httpClient, int timeoutMs) {
        this.config = config;
        this.metrics = metrics;
        this.httpClient = httpClient;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = buildRequest();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = System.currentTimeMillis() - start;
            int status = response.statusCode();
            if (status >= 200 && status < 400) {
                metrics.recordSuccess(latency);
            } else {
                metrics.recordFailure(latency);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            metrics.recordFailure(System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.debug("Request failed: {}", e.getMessage());
            metrics.recordFailure(System.currentTimeMillis() - start);
        }
    }

    private HttpRequest buildRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getTargetUrl()))
                .timeout(Duration.ofMillis(timeoutMs));

        if (config.getHeaders() != null) {
            for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
        }

        String body = config.getRequestBody() != null ? config.getRequestBody() : "";
        HttpRequest.BodyPublisher publisher = body.isBlank()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        switch (config.getHttpMethod()) {
            case GET -> builder.GET();
            case POST -> builder.POST(publisher);
            case PUT -> builder.PUT(publisher);
            case DELETE -> builder.DELETE();
            case PATCH -> builder.method("PATCH", publisher);
        }

        return builder.build();
    }
}
