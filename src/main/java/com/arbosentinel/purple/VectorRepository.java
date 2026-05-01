package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.Vector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorRepository extends JpaRepository<Vector, Integer> {

    Optional<Vector> findByGenusAndSpecies(String genus, String species);

    List<Vector> findByGenus(String genus);

    // All vectors linked to a disease (via disease_vectors join)
    @Query("""
            SELECT v FROM Vector v
            JOIN DiseaseVector dv ON dv.id.vectorId = v.id
            WHERE dv.id.diseaseId = :diseaseId
            ORDER BY dv.isPrimary DESC
            """)
    List<Vector> findByDiseaseId(Integer diseaseId);
}
