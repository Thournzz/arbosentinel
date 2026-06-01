package com.arbosentinel.red;

// ================================================
// RED layer — Dengue business logic
// Powers: Surveillance dashboard (dengue section)
// Data source: DengAI — San Juan, Puerto Rico ('sj') only
//              Iquitos, Peru ('iq') removed — outside Caribbean scope
// Primary Caribbean surveillance: PAHO data via PahoService
// Key features: annual trend, seasonal peaks, climate correlation
// ================================================

import com.arbosentinel.green.dto.DengueAnnualSummaryResponse;
import com.arbosentinel.green.dto.DengueWeeklyCaseResponse;
import com.arbosentinel.green.dto.PagedResponse;
import com.arbosentinel.green.mapper.DengueMapper;
import com.arbosentinel.purple.DengueWeeklyCaseRepository;
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
public class DengueService {

    private final DengueWeeklyCaseRepository dengueRepo;
    private final DengueMapper dengueMapper;

    // 'sj' = San Juan, Puerto Rico — the only DengAI city in the Caribbean
    // 'iq' (Iquitos, Peru) removed — South America, outside Caribbean scope
    private static final List<String> VALID_CITIES = List.of("sj");

    // ── Annual summary (chart data) ──────────────────────────────

    @Cacheable(CacheConfig.DENGUE_ANNUAL)
    public List<DengueAnnualSummaryResponse> getAnnualSummary() {
        log.debug("Building dengue annual summary");
        return dengueRepo.sumAnnualCasesByCity()
            .stream()
            .map(row -> new DengueAnnualSummaryResponse(
                (String) row[0],
                (Integer) row[1],
                row[2] != null ? ((Number) row[2]).longValue() : null
            ))
            .collect(Collectors.toList());
    }

    // ── Seasonal peaks (week-of-year chart) ──────────────────────

    @Cacheable(value = CacheConfig.DENGUE_SEASONAL, key = "#city")
    public List<Object[]> getSeasonalPeaks(String city) {
        validateCity(city);
        return dengueRepo.findPeakWeeksByCity()
            .stream()
            .filter(row -> city.equals(row[0]))
            .collect(Collectors.toList());
    }

    // ── Weekly timeline for a city + year ───────────────────────

    public List<Object[]> getWeeklyTimeline(String city, Integer year) {
        validateCity(city);
        return dengueRepo.findWeeklyTimelineForCityAndYear(city, year);
    }

    // ── Paged weekly case records ────────────────────────────────

    public PagedResponse<DengueWeeklyCaseResponse> getWeeklyCasesByCity(
            String city, int page, int size) {
        validateCity(city);
        var pageable = PageRequest.of(page, size,
            Sort.by("year").descending().and(Sort.by("weekOfYear").ascending()));
        var resultPage = dengueRepo.findByCity(city, pageable);
        return PagedResponse.from(resultPage.map(dengueMapper::toResponse));
    }

    // ── Single week lookup ───────────────────────────────────────

    public DengueWeeklyCaseResponse getWeek(String city, Integer year, Integer week) {
        validateCity(city);
        return dengueRepo.findByCityAndYearAndWeekOfYear(city, year, week)
            .map(dengueMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No dengue data for city=%s year=%d week=%d", city, year, week)));
    }

    // ── City-level totals (hero numbers) ────────────────────────

    @Cacheable(CacheConfig.DENGUE_ANNUAL)
    public List<Object[]> getCityTotals() {
        return dengueRepo.sumTotalCasesByCity();
    }

    // ── Year range for a city ────────────────────────────────────

    public List<DengueWeeklyCaseResponse> getYearRange(
            String city, Integer startYear, Integer endYear) {
        validateCity(city);
        return dengueMapper.toResponseList(
            dengueRepo.findByCityAndYearBetweenOrderByYearAscWeekOfYearAsc(city, startYear, endYear)
        );
    }

    // ── Validation ───────────────────────────────────────────────

    private void validateCity(String city) {
        if (!VALID_CITIES.contains(city.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid city code '" + city + "'. Valid: 'sj' (San Juan, Puerto Rico)");
        }
    }
}
