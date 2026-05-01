package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Brazil SINAN annual count DTO
// Projected from JPQL GROUP BY aggregate
// Powers: Chikungunya / co-disease burden chart
// ================================================

public record SinanCountResponse(
        String diseaseType,
        Integer year,
        Long count
) {}
