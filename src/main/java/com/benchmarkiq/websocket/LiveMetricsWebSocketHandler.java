package com.benchmarkiq.websocket;

import com.benchmarkiq.engine.MetricsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveMetricsWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // executionId -> set of sessions
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long executionId = extractExecutionId(session);
        if (executionId != null) {
            sessions.computeIfAbsent(executionId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket connected for execution {}, session {}", executionId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long executionId = extractExecutionId(session);
        if (executionId != null) {
            CopyOnWriteArraySet<WebSocketSession> executionSessions = sessions.get(executionId);
            if (executionSessions != null) {
                executionSessions.remove(session);
                if (executionSessions.isEmpty()) {
                    sessions.remove(executionId);
                }
            }
        }
        log.debug("WebSocket closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // clients can send ping, no-op
    }

    public void broadcastMetrics(Long executionId, MetricsCollector.SnapshotMetrics snapshot) {
        CopyOnWriteArraySet<WebSocketSession> executionSessions = sessions.get(executionId);
        if (executionSessions == null || executionSessions.isEmpty()) return;

        try {
            MetricsMessage msg = MetricsMessage.builder()
                    .executionId(executionId)
                    .totalRequests(snapshot.getTotalRequests())
                    .successfulRequests(snapshot.getSuccessfulRequests())
                    .failedRequests(snapshot.getFailedRequests())
                    .requestsPerSecond(snapshot.getRequestsPerSecond())
                    .errorRatePercent(snapshot.getErrorRatePercent())
                    .averageLatencyMs(snapshot.getAverageLatencyMs())
                    .currentConcurrentUsers(snapshot.getCurrentConcurrentUsers())
                    .elapsedSeconds(snapshot.getElapsedSeconds())
                    .build();

            String payload = objectMapper.writeValueAsString(msg);
            TextMessage wsMsg = new TextMessage(payload);

            executionSessions.removeIf(s -> !s.isOpen());
            for (WebSocketSession s : executionSessions) {
                if (s.isOpen()) {
                    try {
                        s.sendMessage(wsMsg);
                    } catch (IOException e) {
                        log.warn("Failed to send metrics to session {}: {}", s.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast metrics for execution {}", executionId, e);
        }
    }

    public void broadcastCompletion(Long executionId) {
        CopyOnWriteArraySet<WebSocketSession> executionSessions = sessions.remove(executionId);
        if (executionSessions == null) return;
        try {
            String payload = objectMapper.writeValueAsString(Map.of("type", "COMPLETED", "executionId", executionId));
            TextMessage msg = new TextMessage(payload);
            for (WebSocketSession s : executionSessions) {
                if (s.isOpen()) {
                    try { s.sendMessage(msg); s.close(); } catch (IOException ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast completion for execution {}", executionId, e);
        }
    }

    private Long extractExecutionId(WebSocketSession session) {
        try {
            String path = session.getUri() != null ? session.getUri().getPath() : null;
            if (path == null) return null;
            String[] parts = path.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }
}
