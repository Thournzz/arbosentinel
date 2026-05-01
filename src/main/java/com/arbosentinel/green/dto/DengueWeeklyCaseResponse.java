package com.arbosentinel.green.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// ================================================
// GREEN layer — Dengue weekly case DTO
// Full climate feature set included — used by ML pipeline
// and by detailed surveillance chart
// ================================================

public record DengueWeeklyCaseResponse(
        Integer id,
        String city,
        Integer year,
        Integer weekOfYear,
        LocalDate weekStartDate,
        Integer totalCases,
        BigDecimal ndviNe,
        BigDecimal ndviNw,
        BigDecimal ndviSe,
        BigDecimal ndviSw,
        BigDecimal precipitationAmtMm,
        BigDecimal reanalysisAirTempK,
        BigDecimal reanalysisAvgTempK,
        BigDecimal reanalysisRelativeHumidityPct,
        BigDecimal stationAvgTempC,
        BigDecimal stationMaxTempC,
        BigDecimal stationMinTempC,
        BigDecimal stationPrecipMm
) {}
