package com.arbosentinel.purple;

// ================================================
// PURPLE layer — @Repository
// Entity: BrazilSinanCase (brazil_sinan_cases)
// Patient-level arbovirus surveillance 2013-2021
// Covers dengue, zika, chikungunya — ICD-10 coded
// Powers: Surveillance dashboard, chikungunya section,
//         outbreak outbreak timeline, demographic breakdown
// ================================================

import com.arbosentinel.blue.entity.BrazilSinanCase;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BrazilSinanCaseRepository
        extends JpaRepository<BrazilSinanCase, Integer>,
                JpaSpecificationExecutor<BrazilSinanCase> {

    // ── Basic filters ──────────────────────────────────────────

    Page<BrazilSinanCase> findByDiseaseType(DiseaseType diseaseType, Pageable pageable);

    Page<BrazilSinanCase> findByDiseaseTypeAndYear(DiseaseType diseaseType, Integer year, Pageable pageable);

    List<BrazilSinanCase> findByStateCodeAndDiseaseType(String stateCode, DiseaseType diseaseType);

    Page<BrazilSinanCase> findBySymptomOnsetDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    // ── Aggregate queries ──────────────────────────────────────

    // Annual counts by disease — powers disease burden comparison chart
    @Query("""
            SELECT b.diseaseType, b.year, COUNT(b)
            FROM BrazilSinanCase b
            GROUP BY b.diseaseType, b.year
            ORDER BY b.diseaseType, b.year ASC
            """)
    List<Object[]> countByDiseaseTypeAndYear();

    // State-level disease counts — powers the Brazil choropleth map
    @Query("""
            SELECT b.stateCode, b.stateName, b.diseaseType, COUNT(b)
            FROM BrazilSinanCase b
            WHERE b.diseaseType = :diseaseType AND b.year = :year
            GROUP BY b.stateCode, b.stateName, b.diseaseType
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> countByStateForDiseaseAndYear(DiseaseType diseaseType, Integer year);

    // Sex breakdown — male vs female case distribution
    @Query("""
            SELECT b.sex, COUNT(b)
            FROM BrazilSinanCase b
            WHERE b.diseaseType = :diseaseType AND b.sex IS NOT NULL
            GROUP BY b.sex
            """)
    List<Object[]> countBySexForDisease(DiseaseType diseaseType);

    // Outcome distribution (cure/death/ongoing/unknown) per disease
    @Query("""
            SELECT b.outcome, COUNT(b)
            FROM BrazilSinanCase b
            WHERE b.diseaseType = :diseaseType AND b.outcome IS NOT NULL
            GROUP BY b.outcome
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> countByOutcomeForDisease(DiseaseType diseaseType);

    // Weekly notification trend — for outbreak wave detection
    @Query("""
            SELECT b.year, b.notificationWeek, COUNT(b)
            FROM BrazilSinanCase b
            WHERE b.diseaseType = :diseaseType AND b.year = :year
            AND b.notificationWeek IS NOT NULL
            GROUP BY b.year, b.notificationWeek
            ORDER BY b.notificationWeek ASC
            """)
    List<Object[]> weeklyNotificationCountForDiseaseAndYear(DiseaseType diseaseType, Integer year);
}
