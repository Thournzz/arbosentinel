package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — West Nile hospitalization DTO
// Neuroinvasive vs non-neuroinvasive breakdown
// Powers: Clinical severity chart on West Nile section
// ================================================

public record WestNileHospitalizationResponse(
        Integer id,
        Integer year,
        Integer neuroinvasiveCases,
        Integer nonNeuroinvasiveCases
) {}
