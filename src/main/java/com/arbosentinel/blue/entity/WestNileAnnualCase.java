package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "west_nile_annual_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WestNileAnnualCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "year", nullable = false, unique = true)
    private Integer year;

    @Column(name = "reported_cases", nullable = false)
    private Integer reportedCases;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
