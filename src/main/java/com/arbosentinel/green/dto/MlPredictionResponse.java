package com.arbosentinel.green.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ================================================
// GREEN layer — ML prediction result DTO
// Returned after calling Python FastAPI microservice
// and storing result in ml_predictions table
// ================================================

public record MlPredictionResponse(
        Integer id,
        String diseaseType,
        String regionName,
        LocalDateTime predictionDate,
        Integer weekOfYear,
        BigDecimal avgTempC,
        BigDecimal precipitationMm,
        BigDecimal humidityPercent,
        BigDecimal ndvi,
        Integer predictedCases,
        BigDecimal riskScore,
        BigDecimal confidencePercent,
        String modelVersion
) {}
