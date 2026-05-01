package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: disease_vectors table (V2)
// Many-to-many join: diseases <-> vectors
// Composite PK: (disease_id, vector_id)
// is_primary: TRUE = primary vector for this disease
// Note: V6 adds Aedes vittatus with Sandiford et al. 2025 notes
// ================================================

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disease_vectors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiseaseVector {

    @EmbeddedId
    private DiseaseVectorId id;

    // TRUE = this vector is the primary/dominant transmission route
    @Column(name = "is_primary")
    private Boolean isPrimary;

    // e.g. "vector competence under investigation (Sandiford et al. 2025)"
    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
