package com.arbosentinel.green.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ================================================
// GREEN layer — Outbreak risk score DTO
// contributingFactors: JSON string passed through as-is
// riskLevel: "low" | "moderate" | "high" | "critical"
// Powers: Risk overview panel + OutbreakRadar component
// ================================================

public record OutbreakRiskResponse(
        Integer id,
        String diseaseType,
        String regionName,
        LocalDateTime computedAt,
        BigDecimal riskScore,
        String riskLevel,
        String contributingFactors,
        LocalDateTime expiresAt
) {}
