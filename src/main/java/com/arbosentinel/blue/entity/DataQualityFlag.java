package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: data_quality_flags table (V1)
// Flags individual rows with data quality issues
// Used by WHITE validation layer + ORANGE ETL jobs
// ================================================

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_quality_flags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataQualityFlag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Name of the table that contains the flagged row
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    // Primary key of the flagged row in that table
    @Column(name = "row_id", nullable = false)
    private Integer rowId;

    // e.g. "MISSING_VALUE", "OUT_OF_RANGE", "DUPLICATE", "INVALID_DATE"
    @Column(name = "flag_type", nullable = false, length = 100)
    private String flagType;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "flagged_value", columnDefinition = "text")
    private String flaggedValue;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    @Column(name = "resolved")
    private Boolean resolved;
}
