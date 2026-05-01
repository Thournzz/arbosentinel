package com.arbosentinel.red;

// ================================================
// RED layer — Malaria business logic
// Powers: Malaria section of Surveillance page
// Sources: WHO estimated (confidence intervals)
//          + WHO reported cases
// Key features: global burden timeline, regional breakdown,
//               top-burden countries per year
// ================================================

import com.arbosentinel.green.dto.MalariaBurdenResponse;
import com.arbosentinel.green.dto.MalariaEstimatedResponse;
import com.arbosentinel.green.dto.PagedResponse;
import com.arbosentinel.green.mapper.MalariaMapper;
import com.arbosentinel.purple.MalariaEstimatedCaseRepository;
import com.arbosentinel.purple.MalariaReportedCaseRepository;
import com.arbosentinel.white.ResourceNotFoundException;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MalariaService {

    private final MalariaEstimatedCaseRepository estimatedRepo;
    private final MalariaReportedCaseRepository reportedRepo;
    private final MalariaMapper malariaMapper;

    // ── Global burden by year (timeline chart) ───────────────────

    @Cacheable(CacheConfig.MALARIA_BURDEN)
    public List<MalariaBurdenResponse> getGlobalBurdenByYear() {
        log.debug("Building global malaria burden by year");
        return estimatedRepo.sumGlobalBurdenByYear()
            .stream()
            .map(row -> new MalariaBurdenResponse(
                (Integer) row[0],
                row[1] != null ? ((Number) row[1]).longValue() : null,
                row[2] != null ? ((Number) row[2]).longValue() : null
            ))
            .collect(Collectors.toList());
    }

    // ── Regional burden (WHO region breakdown) ───────────────────

    @Cacheable(CacheConfig.MALARIA_REGION)
    public List<Object[]> getBurdenByRegion() {
        return estimatedRepo.sumByWhoRegionAndYear();
    }

    // ── Top countries by burden for a given year ─────────────────

    public List<MalariaEstimatedResponse> getTopCountriesByYear(Integer year, int limit) {
        var pageable = PageRequest.of(0, limit);
        return malariaMapper.toResponseList(
            estimatedRepo.findTopCountriesByYear(year, pageable)
        );
    }

    // ── Country history (time series) ────────────────────────────

    public List<MalariaEstimatedResponse> getCountryHistory(String country) {
        var results = estimatedRepo.findByCountryOrderByYearAsc(country);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Malaria", "country", country);
        }
        return malariaMapper.toResponseList(results);
    }

    // ── Single country + year ────────────────────────────────────

    public MalariaEstimatedResponse getCountryYear(String country, Integer year) {
        return estimatedRepo.findByCountryAndYear(country, year)
            .map(malariaMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No malaria data for country=%s year=%d", country, year)));
    }

    // ── Paged country list for a given year ─────────────────────

    public PagedResponse<MalariaEstimatedResponse> getByYear(Integer year, int page, int size) {
        var pageable = PageRequest.of(page, size,
            Sort.by("casesMedian").descending());
        return PagedResponse.from(
            estimatedRepo.findByYear(year, pageable)
                .map(malariaMapper::toResponse)
        );
    }
}
