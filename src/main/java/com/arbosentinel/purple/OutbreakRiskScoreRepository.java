package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.OutbreakRiskScore;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.blue.entity.enums.SeverityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutbreakRiskScoreRepository extends JpaRepository<OutbreakRiskScore, Integer> {

    // Current active score for a disease (not yet expired)
    @Query("""
            SELECT o FROM OutbreakRiskScore o
            WHERE o.diseaseType = :diseaseType
            AND (o.expiresAt IS NULL OR o.expiresAt > :now)
            ORDER BY o.computedAt DESC
            LIMIT 1
            """)
    Optional<OutbreakRiskScore> findCurrentByDiseaseType(DiseaseType diseaseType, LocalDateTime now);

    // All active scores — powers the risk overview panel
    @Query("""
            SELECT o FROM OutbreakRiskScore o
            WHERE (o.expiresAt IS NULL OR o.expiresAt > :now)
            ORDER BY o.riskScore DESC
            """)
    List<OutbreakRiskScore> findAllActive(LocalDateTime now);

    // All scores at or above a given risk level
    List<OutbreakRiskScore> findByRiskLevelOrderByComputedAtDesc(SeverityLevel riskLevel);

    // Historical scores for a disease — powers the risk trend chart
    List<OutbreakRiskScore> findByDiseaseTypeOrderByComputedAtAsc(DiseaseType diseaseType);
}
