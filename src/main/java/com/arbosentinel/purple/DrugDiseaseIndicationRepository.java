package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.DrugDiseaseIndication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugDiseaseIndicationRepository extends JpaRepository<DrugDiseaseIndication, Integer> {

    List<DrugDiseaseIndication> findByDiseaseId(Integer diseaseId);

    List<DrugDiseaseIndication> findByDrugId(Integer drugId);

    List<DrugDiseaseIndication> findByDiseaseIdAndIndicationType(Integer diseaseId, String indicationType);

    List<DrugDiseaseIndication> findByDiseaseIdOrderByEvidenceLevelAsc(Integer diseaseId);
}
