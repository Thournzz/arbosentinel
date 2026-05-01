package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.IngestionLog;
import com.arbosentinel.blue.entity.enums.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngestionLogRepository extends JpaRepository<IngestionLog, Integer> {

    List<IngestionLog> findByDataSourceIdOrderByRunAtDesc(Integer dataSourceId);

    // Most recent run for a data source — used by ETL to check if re-run is needed
    Optional<IngestionLog> findTopByDataSourceIdOrderByRunAtDesc(Integer dataSourceId);

    List<IngestionLog> findByStatus(IngestionStatus status);

    // Count failed runs in the last batch — used by Mr. Prog health alerts
    @Query("SELECT COUNT(l) FROM IngestionLog l WHERE l.status = 'failed'")
    long countFailedRuns();
}
