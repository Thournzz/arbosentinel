package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "malaria_reported_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MalariaReportedCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "year", nullable = false)
    private Integer year;

    // DECIMAL(15,1) — can be 0.0 or fractional due to WHO rounding conventions
    @Column(name = "reported_cases", precision = 15, scale = 1)
    private BigDecimal reportedCases;

    @Column(name = "reported_deaths", precision = 15, scale = 1)
    private BigDecimal reportedDeaths;

    @Column(name = "who_region")
    private String whoRegion;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
