package com.benchmarkiq.repository;

import com.benchmarkiq.entity.TestResult;
import com.benchmarkiq.entity.ThresholdResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    Optional<TestResult> findByTestExecutionId(Long executionId);

    @Query("SELECT tr FROM TestResult tr " +
           "WHERE tr.testExecution.testConfig.id = :configId " +
           "ORDER BY tr.completedAt DESC")
    Page<TestResult> findByConfigIdOrderByCompletedAtDesc(@Param("configId") Long configId, Pageable pageable);

    @Query("SELECT tr FROM TestResult tr " +
           "WHERE tr.testExecution.testConfig.id = :configId " +
           "ORDER BY tr.completedAt DESC")
    List<TestResult> findLatestByConfigId(@Param("configId") Long configId, Pageable pageable);

    @Query("SELECT AVG(tr.averageLatencyMs) FROM TestResult tr " +
           "WHERE tr.testExecution.testConfig.id = :configId " +
           "AND tr.completedAt >= :since")
    Double findAverageLatencyByConfigIdSince(@Param("configId") Long configId,
                                              @Param("since") LocalDateTime since);

    @Query("SELECT tr FROM TestResult tr " +
           "WHERE tr.testExecution.testConfig.id = :configId " +
           "AND tr.completedAt BETWEEN :from AND :to " +
           "ORDER BY tr.completedAt ASC")
    List<TestResult> findTrendData(@Param("configId") Long configId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.thresholdResult = :result")
    long countByThresholdResult(@Param("result") ThresholdResult result);

    void deleteByTestExecutionStartedAtBefore(LocalDateTime before);
}
