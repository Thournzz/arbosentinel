package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: brazil_sinan_cases table (V3)
// Source: Brazil SINAN arbovirus surveillance 2013-2021
// Covers: Dengue, Zika, Chikungunya (patient-level)
// ================================================

import com.arbosentinel.blue.entity.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "brazil_sinan_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrazilSinanCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "disease_type", nullable = false, columnDefinition = "disease_type")
    private DiseaseType diseaseType;

    // 1=dengue, 2=dengue+warning signs, 3=severe dengue
    @Column(name = "classification")
    private Integer classification;

    @Column(name = "municipality_code")
    private Long municipalityCode;

    // 'M' or 'F' — stored as String, not char, for JPA compatibility
    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "notification_date")
    private LocalDate notificationDate;

    @Column(name = "symptom_onset_date")
    private LocalDate symptomOnsetDate;

    // 1=cure, 2=death, 3=ongoing, 4=unknown
    @Column(name = "outcome")
    private Integer outcome;

    // ICD-10: A90=dengue, A92.0=chikungunya, A92.5=zika
    @Column(name = "disease_code", length = 10)
    private String diseaseCode;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "municipality_name", length = 200)
    private String municipalityName;

    @Column(name = "state_name", length = 100)
    private String stateName;

    @Column(name = "year")
    private Integer year;

    // SINAN encodes patient age in days (e.g. 10950 = ~30 years)
    @Column(name = "age_days")
    private Integer ageDays;

    @Column(name = "notification_week")
    private Integer notificationWeek;

    @Column(name = "symptom_onset_week")
    private Integer symptomOnsetWeek;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
