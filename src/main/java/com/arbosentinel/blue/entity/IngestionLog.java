package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: ingestion_logs table (V1)
// One row per ETL run — full data lineage trail
// ================================================

import com.arbosentinel.blue.entity.enums.IngestionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngestionLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    @Column(name = "rows_processed")
    private Integer rowsProcessed;

    @Column(name = "rows_inserted")
    private Integer rowsInserted;

    @Column(name = "rows_skipped")
    private Integer rowsSkipped;

    @Column(name = "rows_failed")
    private Integer rowsFailed;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "ingestion_status")
    private IngestionStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
