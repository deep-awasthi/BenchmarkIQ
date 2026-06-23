package com.benchmarkiq.repository;

import com.benchmarkiq.entity.TestConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestConfigRepository extends JpaRepository<TestConfig, Long> {

    Page<TestConfig> findByCreatedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT tc FROM TestConfig tc WHERE " +
           "(:userId IS NULL OR tc.createdBy.id = :userId) AND " +
           "(:name IS NULL OR LOWER(tc.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<TestConfig> findWithFilters(@Param("userId") Long userId,
                                    @Param("name") String name,
                                    Pageable pageable);

    List<TestConfig> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
}
