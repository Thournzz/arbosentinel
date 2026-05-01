package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.DiseaseVector;
import com.arbosentinel.blue.entity.DiseaseVectorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseVectorRepository extends JpaRepository<DiseaseVector, DiseaseVectorId> {

    List<DiseaseVector> findById_DiseaseId(Integer diseaseId);

    List<DiseaseVector> findById_VectorId(Integer vectorId);

    List<DiseaseVector> findById_DiseaseIdAndIsPrimary(Integer diseaseId, Boolean isPrimary);
}
