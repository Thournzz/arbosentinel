package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Embeddable composite key
// Used by: DiseaseVector (disease_vectors table, V2)
// PRIMARY KEY (disease_id, vector_id)
// ================================================

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class DiseaseVectorId implements Serializable {

    @Column(name = "disease_id")
    private Integer diseaseId;

    @Column(name = "vector_id")
    private Integer vectorId;
}
