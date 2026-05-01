package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: clinical_symptom_profiles table (V4)
// Powers: Pathogen Library clinical detail cards
// Source: French clinical vector-borne disease dataset
// ================================================

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "clinical_symptom_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalSymptomProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "disease_id")
    private Integer diseaseId;

    @Column(name = "symptom_name_en", nullable = false, length = 200)
    private String symptomNameEn;

    // French translation — bilingual dataset origin
    @Column(name = "symptom_name_fr", length = 200)
    private String symptomNameFr;

    // 'acute', 'complication', 'recovery'
    @Column(name = "phase", length = 50)
    private String phase;

    // e.g. 98.0 = present in 98% of confirmed cases
    @Column(name = "prevalence_percent", precision = 5, scale = 2)
    private BigDecimal prevalencePercent;

    // TRUE = pathognomonic (definitively diagnostic of this disease)
    @Column(name = "is_pathognomonic")
    private Boolean isPathognomonic;

    @Column(name = "clinical_significance", length = 200)
    private String clinicalSignificance;
}
