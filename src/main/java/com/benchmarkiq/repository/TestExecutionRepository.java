package com.benchmarkiq.repository;

import com.benchmarkiq.entity.ExecutionStatus;
import com.benchmarkiq.entity.TestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    @EntityGraph(attributePaths = {"testConfig", "triggeredBy", "testResult"})
    List<TestExecution> findByStatus(ExecutionStatus status);

    @EntityGraph(attributePaths = {"testConfig", "triggeredBy", "testResult"})
    Page<TestExecution> findByTestConfigIdOrderByStartedAtDesc(Long configId, Pageable pageable);

    Page<TestExecution> findByTriggeredByIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<TestExecution> findTopByTestConfigIdOrderByStartedAtDesc(Long configId);

    @Query("SELECT te FROM TestExecution te WHERE te.status = :status AND te.startedAt < :before")
    List<TestExecution> findByStatusAndStartedAtBefore(@Param("status") ExecutionStatus status,
                                                        @Param("before") LocalDateTime before);

    @Query("SELECT COUNT(te) FROM TestExecution te WHERE te.status = :status")
    long countByStatus(@Param("status") ExecutionStatus status);

    @Query("SELECT te FROM TestExecution te " +
           "WHERE te.testConfig.createdBy.id = :userId " +
           "ORDER BY te.startedAt DESC")
    Page<TestExecution> findByOwnerUserId(@Param("userId") Long userId, Pageable pageable);

    List<TestExecution> findByStartedAtBefore(LocalDateTime before);

    @EntityGraph(attributePaths = {"testConfig", "triggeredBy", "testResult"})
    Optional<TestExecution> findWithDetailsById(Long id);
}
