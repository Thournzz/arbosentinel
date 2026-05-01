package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "west_nile_hospitalizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WestNileHospitalization {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "year", nullable = false, unique = true)
    private Integer year;

    @Column(name = "neuroinvasive_cases")
    private Integer neuroinvasiveCases;

    @Column(name = "non_neuroinvasive_cases")
    private Integer nonNeuroinvasiveCases;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
