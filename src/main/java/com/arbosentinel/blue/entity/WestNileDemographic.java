package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "west_nile_demographics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WestNileDemographic {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // e.g. "0-9", "10-19", "20-29", ..., "70+"
    @Column(name = "age_group", nullable = false, length = 20)
    private String ageGroup;

    // Incidence rate per 100,000 population — male
    @Column(name = "male_rate", precision = 8, scale = 4)
    private BigDecimal maleRate;

    // Incidence rate per 100,000 population — female
    @Column(name = "female_rate", precision = 8, scale = 4)
    private BigDecimal femaleRate;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
