package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "west_nile_state_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WestNileStateCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "case_type", nullable = false, length = 50)
    private String caseType;

    // e.g. "1999-2024" — stored as string in source data
    @Column(name = "year_range", nullable = false, length = 20)
    private String yearRange;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "reported_cases")
    private Integer reportedCases;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
