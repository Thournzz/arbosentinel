package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.Disease;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Integer> {

    Optional<Disease> findByDiseaseType(DiseaseType diseaseType);

    boolean existsByDiseaseType(DiseaseType diseaseType);
}
