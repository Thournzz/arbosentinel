package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.MlPrediction;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MlPredictionRepository extends JpaRepository<MlPrediction, Integer> {

    List<MlPrediction> findByDiseaseTypeOrderByPredictionDateDesc(DiseaseType diseaseType);

    // Most recent prediction for a disease+region pair
    Optional<MlPrediction> findTopByDiseaseTypeAndRegionNameOrderByPredictionDateDesc(
            DiseaseType diseaseType, String regionName);

    // Latest N predictions — for the AI forecast panel on dashboard
    @Query("""
            SELECT m FROM MlPrediction m
            WHERE m.diseaseType = :diseaseType
            ORDER BY m.predictionDate DESC
            """)
    List<MlPrediction> findLatestPredictions(DiseaseType diseaseType, Pageable pageable);

    // Predictions above risk threshold — triggers Mr. Prog alerts
    @Query("""
            SELECT m FROM MlPrediction m
            WHERE m.riskScore >= :threshold
            ORDER BY m.riskScore DESC
            """)
    List<MlPrediction> findHighRiskPredictions(double threshold);
}
