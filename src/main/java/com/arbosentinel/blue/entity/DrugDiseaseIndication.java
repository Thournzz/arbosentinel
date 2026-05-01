package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: drug_disease_indications table (V4)
// Join: pharmacology_drugs <-> diseases
// indication_type: treatment | prophylaxis | supportive
// evidence_level: who_recommended | first_line | second_line | adjunct
// ================================================

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drug_disease_indications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DrugDiseaseIndication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "drug_id")
    private Integer drugId;

    @Column(name = "disease_id")
    private Integer diseaseId;

    // 'treatment', 'prophylaxis', 'supportive'
    @Column(name = "indication_type", length = 50)
    private String indicationType;

    // 'who_recommended', 'first_line', 'second_line', 'adjunct'
    @Column(name = "evidence_level", length = 100)
    private String evidenceLevel;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
