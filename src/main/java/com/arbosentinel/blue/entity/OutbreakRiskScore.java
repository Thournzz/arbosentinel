package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: outbreak_risk_scores table (V5)
// Computed by RED service layer on @Scheduled cadence
// contributing_factors: JSONB — e.g. {"trend_pct":40,"ndvi":0.82,"cases_ytd":2847}
// ================================================

import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.blue.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbreak_risk_scores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutbreakRiskScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "disease_type", nullable = false, columnDefinition = "disease_type")
    private DiseaseType diseaseType;

    @Column(name = "region_name", length = 200)
    private String regionName;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    // Composite risk score 0-100
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", columnDefinition = "severity_level")
    private SeverityLevel riskLevel;

    // JSONB breakdown of what drove the score — e.g. trend %, NDVI, YTD cases
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_factors", columnDefinition = "jsonb")
    private String contributingFactors;

    // Cache expiry — risk scores recalculated on schedule, old ones expire
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
