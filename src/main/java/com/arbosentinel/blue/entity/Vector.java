package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: vectors table (V2)
// Powers: Pathogen Library vector section
// ================================================

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vectors")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "genus", nullable = false)
    private String genus;

    @Column(name = "species", nullable = false)
    private String species;

    @Column(name = "common_name")
    private String commonName;

    @Column(name = "geographic_range", columnDefinition = "TEXT")
    private String geographicRange;

    @Column(name = "breeding_conditions", columnDefinition = "TEXT")
    private String breedingConditions;

    @Column(name = "activity_peak")
    private String activityPeak;
}
