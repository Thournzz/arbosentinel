package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.ClinicalSymptomProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicalSymptomProfileRepository extends JpaRepository<ClinicalSymptomProfile, Integer> {

    List<ClinicalSymptomProfile> findByDiseaseIdOrderByPrevalencePercentDesc(Integer diseaseId);

    List<ClinicalSymptomProfile> findByDiseaseIdAndPhase(Integer diseaseId, String phase);

    // Pathognomonic symptoms only — used for differential diagnosis widget
    List<ClinicalSymptomProfile> findByDiseaseIdAndIsPathognomonicTrue(Integer diseaseId);

    // All pathognomonic symptoms across all diseases — for comparison table
    @Query("""
            SELECT c FROM ClinicalSymptomProfile c
            WHERE c.isPathognomonic = TRUE
            ORDER BY c.diseaseId, c.prevalencePercent DESC
            """)
    List<ClinicalSymptomProfile> findAllPathognomonic();
}
