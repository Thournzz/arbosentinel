package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Mosquito vector DTO
// Includes isPrimary + notes from disease_vectors join
// when returned in context of a disease
// ================================================

public record VectorResponse(
        Integer id,
        String genus,
        String species,
        String commonName,
        String geographicRange,
        String breedingConditions,
        String activityPeak,
        Boolean isPrimary,   // null when returned outside disease context
        String notes         // e.g. "vector competence under investigation (Sandiford et al. 2025)"
) {}
