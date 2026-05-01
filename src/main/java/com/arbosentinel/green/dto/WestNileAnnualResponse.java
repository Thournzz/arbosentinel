package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — West Nile annual case DTO
// Powers: West Nile trend chart on Surveillance page
// ================================================

public record WestNileAnnualResponse(
        Integer id,
        Integer year,
        Integer reportedCases
) {}
