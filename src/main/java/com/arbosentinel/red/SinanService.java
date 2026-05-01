package com.arbosentinel.red;

// ================================================
// RED layer — Brazil SINAN business logic
// Powers: Chikungunya section + co-disease section
// Patient-level data: dengue, zika, chikungunya 2013-2021
// ================================================

import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.green.dto.SinanCountResponse;
import com.arbosentinel.purple.BrazilSinanCaseRepository;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SinanService {

    private final BrazilSinanCaseRepository sinanRepo;

    // ── Annual counts by disease — chart data ────────────────────

    @Cacheable(CacheConfig.SINAN_COUNTS)
    public List<SinanCountResponse> getAnnualCountsByDisease() {
        log.debug("Building SINAN annual counts");
        return sinanRepo.countByDiseaseTypeAndYear()
            .stream()
            .map(row -> new SinanCountResponse(
                row[0].toString(),
                (Integer) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    // ── State-level counts for a disease and year ────────────────

    public List<Object[]> getStateCountsByDiseaseAndYear(String diseaseType, Integer year) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return sinanRepo.countByStateForDiseaseAndYear(type, year);
    }

    // ── Outcome distribution for a disease ──────────────────────

    public List<Object[]> getOutcomeDistribution(String diseaseType) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return sinanRepo.countByOutcomeForDisease(type);
    }

    // ── Sex breakdown for a disease ──────────────────────────────

    public List<Object[]> getSexBreakdown(String diseaseType) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return sinanRepo.countBySexForDisease(type);
    }

    // ── Weekly notification timeline for outbreak detection ──────

    public List<Object[]> getWeeklyTimeline(String diseaseType, Integer year) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return sinanRepo.weeklyNotificationCountForDiseaseAndYear(type, year);
    }
}
