package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: ml_predictions table (V5)
// Stores results from Python FastAPI scikit-learn model
// input_payload: full JSONB snapshot for audit trail
// ================================================

import com.arbosentinel.blue.entity.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_predictions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MlPrediction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "disease_type", nullable = false, columnDefinition = "disease_type")
    private DiseaseType diseaseType;

    @Column(name = "region_name", length = 200)
    private String regionName;

    @Column(name = "prediction_date")
    private LocalDateTime predictionDate;

    @Column(name = "week_of_year")
    private Integer weekOfYear;

    @Column(name = "avg_temp_c", precision = 8, scale = 4)
    private BigDecimal avgTempC;

    @Column(name = "precipitation_mm", precision = 10, scale = 4)
    private BigDecimal precipitationMm;

    @Column(name = "humidity_percent", precision = 8, scale = 4)
    private BigDecimal humidityPercent;

    @Column(name = "ndvi", precision = 10, scale = 7)
    private BigDecimal ndvi;

    @Column(name = "predicted_cases")
    private Integer predictedCases;

    // Risk score 0-100
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    // Model confidence 0-100
    @Column(name = "confidence_percent", precision = 5, scale = 2)
    private BigDecimal confidencePercent;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    // Full input snapshot — JSONB. Hibernate 6 native JSON type mapping.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_payload", columnDefinition = "jsonb")
    private String inputPayload;
}
