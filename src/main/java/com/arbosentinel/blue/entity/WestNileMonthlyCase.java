package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "west_nile_monthly_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WestNileMonthlyCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "case_type", nullable = false, length = 50)
    private String caseType;

    // e.g. "1999-2024"
    @Column(name = "year_range", nullable = false, length = 20)
    private String yearRange;

    @Column(name = "month", nullable = false, length = 10)
    private String month;

    @Column(name = "reported_cases")
    private Integer reportedCases;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
