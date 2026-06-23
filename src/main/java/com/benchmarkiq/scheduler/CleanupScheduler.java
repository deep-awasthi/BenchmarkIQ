package com.benchmarkiq.scheduler;

import com.benchmarkiq.entity.ExecutionStatus;
import com.benchmarkiq.repository.TestExecutionRepository;
import com.benchmarkiq.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;

    @Value("${app.cleanup.max-result-age-days:30}")
    private int maxResultAgeDays;

    @Scheduled(cron = "${app.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(maxResultAgeDays);
        log.info("Running cleanup: removing results older than {} days (before {})", maxResultAgeDays, cutoff);

        List<com.benchmarkiq.entity.TestExecution> oldExecutions =
                executionRepository.findByStartedAtBefore(cutoff);

        if (oldExecutions.isEmpty()) {
            log.info("No old test results to clean up");
            return;
        }

        log.info("Deleting {} old test executions and their results", oldExecutions.size());
        executionRepository.deleteAll(oldExecutions);
        log.info("Cleanup completed successfully");
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void recoverStuckTests() {
        LocalDateTime stuckThreshold = LocalDateTime.now().minusHours(2);
        List<com.benchmarkiq.entity.TestExecution> stuck =
                executionRepository.findByStatusAndStartedAtBefore(ExecutionStatus.RUNNING, stuckThreshold);

        if (!stuck.isEmpty()) {
            log.warn("Found {} stuck test executions, marking as FAILED", stuck.size());
            stuck.forEach(e -> {
                e.setStatus(ExecutionStatus.FAILED);
                e.setErrorMessage("Test timed out — marked as FAILED by cleanup scheduler");
                e.setCompletedAt(LocalDateTime.now());
            });
            executionRepository.saveAll(stuck);
        }
    }
}
