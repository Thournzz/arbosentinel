package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "malaria_estimated_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MalariaEstimatedCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "cases_median")
    private Long casesMedian;

    @Column(name = "cases_min")
    private Long casesMin;

    @Column(name = "cases_max")
    private Long casesMax;

    @Column(name = "deaths_median")
    private Integer deathsMedian;

    @Column(name = "deaths_min")
    private Integer deathsMin;

    @Column(name = "deaths_max")
    private Integer deathsMax;

    @Column(name = "who_region")
    private String whoRegion;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
