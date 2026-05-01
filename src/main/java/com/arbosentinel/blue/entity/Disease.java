package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: diseases table (V2)
// Powers: Pathogen Library page
// ================================================

import com.arbosentinel.blue.entity.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diseases")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "disease_type", nullable = false, unique = true,
            columnDefinition = "disease_type")
    private DiseaseType diseaseType;

    @Column(name = "common_name")
    private String commonName;

    @Column(name = "pathogen_family")
    private String pathogenFamily;

    @Column(name = "pathogen_species")
    private String pathogenSpecies;

    @Column(name = "genome_type")
    private String genomeType;

    @Column(name = "structure")
    private String structure;

    @Column(name = "transmission_route")
    private String transmissionRoute;

    @Column(name = "first_identified_year")
    private Integer firstIdentifiedYear;

    @Column(name = "first_identified_location")
    private String firstIdentifiedLocation;

    @Column(name = "who_classification")
    private String whoClassification;

    @Column(name = "incubation_min_days")
    private Integer incubationMinDays;

    @Column(name = "incubation_max_days")
    private Integer incubationMaxDays;

    @Column(name = "acute_phase_description", columnDefinition = "TEXT")
    private String acutePhaseDescription;

    @Column(name = "complications", columnDefinition = "TEXT")
    private String complications;

    @Column(name = "recovery_description", columnDefinition = "TEXT")
    private String recoveryDescription;

    @Column(name = "treatment_summary", columnDefinition = "TEXT")
    private String treatmentSummary;
}
