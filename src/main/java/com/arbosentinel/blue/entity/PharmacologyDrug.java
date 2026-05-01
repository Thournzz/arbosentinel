package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: pharmacology_drugs table (V4)
// Powers: Pharmacology page
// Dedicated to Dr. Simone Sandiford
// ================================================

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacology_drugs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PharmacologyDrug {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "drug_class", length = 100)
    private String drugClass;

    @Column(name = "mechanism_of_action", columnDefinition = "text")
    private String mechanismOfAction;

    @Column(name = "dosing_adult", columnDefinition = "text")
    private String dosingAdult;

    @Column(name = "dosing_pediatric", columnDefinition = "text")
    private String dosingPediatric;

    @Column(name = "key_interactions", columnDefinition = "text")
    private String keyInteractions;

    @Column(name = "contraindications", columnDefinition = "text")
    private String contraindications;

    // WHO Essential Medicines List inclusion
    @Column(name = "who_essential_medicine")
    private Boolean whoEssentialMedicine;

    // Flags drugs with significant interaction risk — powers UI warning badge
    @Column(name = "interaction_warning")
    private Boolean interactionWarning;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
