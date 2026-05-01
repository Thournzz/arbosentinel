package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.PharmacologyDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacologyDrugRepository extends JpaRepository<PharmacologyDrug, Integer> {

    List<PharmacologyDrug> findByDrugClass(String drugClass);

    List<PharmacologyDrug> findByWhoEssentialMedicineTrue();

    List<PharmacologyDrug> findByInteractionWarningTrue();

    // All drugs indicated for a specific disease — joins through drug_disease_indications
    @Query("""
            SELECT pd FROM PharmacologyDrug pd
            JOIN DrugDiseaseIndication ddi ON ddi.drugId = pd.id
            WHERE ddi.diseaseId = :diseaseId
            ORDER BY ddi.evidenceLevel, pd.drugName
            """)
    List<PharmacologyDrug> findByDiseaseId(Integer diseaseId);

    // Drugs for a disease with a specific indication type (treatment/prophylaxis/supportive)
    @Query("""
            SELECT pd FROM PharmacologyDrug pd
            JOIN DrugDiseaseIndication ddi ON ddi.drugId = pd.id
            WHERE ddi.diseaseId = :diseaseId AND ddi.indicationType = :indicationType
            ORDER BY ddi.evidenceLevel
            """)
    List<PharmacologyDrug> findByDiseaseIdAndIndicationType(Integer diseaseId, String indicationType);
}
