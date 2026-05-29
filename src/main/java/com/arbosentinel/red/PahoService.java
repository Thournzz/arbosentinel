package com.arbosentinel.red;

// ══════════════════════════════════════════════════════════════════════════════
// RED layer — PAHO Caribbean surveillance business logic
//
// LEARNING NOTE — What the service layer is responsible for:
//   The service is the ONLY layer that contains business logic.
//   "Business logic" means anything beyond a simple data read or write:
//   - Validation ("is this a valid Caribbean country code?")
//   - Data transformation ("convert entity fields to the DTO shape the frontend needs")
//   - Orchestration ("trigger ETL, then return the result count")
//   - Error handling ("if the country has no data, throw ResourceNotFoundException")
//
//   The controller (blue) handles HTTP only: parse the request, call the service, return the response.
//   The repository (purple) handles SQL only: run the query, return the entity.
//   The service (red) handles EVERYTHING IN BETWEEN.
//
// LEARNING NOTE — @Transactional(readOnly = true) on the class:
//   Adding this annotation at CLASS level means every method in this class defaults
//   to a read-only transaction — Hibernate optimises reads (no dirty checking, etc.).
//   Methods that WRITE data (like triggerRefresh) override this with
//   @Transactional (no readOnly) on the specific method.
//   This pattern avoids accidentally starting a read-write transaction for every
//   method call — keeps the database session lean.
// ══════════════════════════════════════════════════════════════════════════════

import com.arbosentinel.blue.entity.PahoCaribCase;
import com.arbosentinel.green.dto.PahoCaribCaseResponse;
import com.arbosentinel.orange.PahoEtlJob;
import com.arbosentinel.purple.PahoCaribCaseRepository;
import com.arbosentinel.white.ResourceNotFoundException;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PahoService {

    private final PahoCaribCaseRepository pahoRepo;
    private final PahoEtlJob pahoEtlJob;

    // Valid country codes — guards against bad API requests before hitting the DB
    private static final Set<String> VALID_LOCATIONS = Set.of(
            "jm", "tt", "bb", "cu", "do", "ht", "pr", "bs", "gy", "bz"
    );

    // ── Per-country queries ────────────────────────────────────────────────────

    // LEARNING NOTE — @Cacheable with a dynamic key:
    //   @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "#locationCode")
    //   "value" = the cache name (must be registered in CacheConfig.cacheManager())
    //   "key" = the cache key expression (Spring EL) — "#locationCode" means
    //   "use the value of the locationCode parameter as the cache key".
    //   So cache entry "jm" holds Jamaica's data, "tt" holds Trinidad's data.
    //   When the method is called with "jm" twice, the second call hits the cache
    //   instead of querying the DB. Cache expires after 300 seconds (set in CacheConfig).
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "#locationCode")
    public List<PahoCaribCaseResponse> getByCountry(String locationCode) {
        validateLocation(locationCode);

        List<PahoCaribCase> cases = pahoRepo.findByLocationCodeOrderByEpiWeekAsc(locationCode);
        log.debug("PahoService.getByCountry: {} — {} records", locationCode, cases.size());

        return cases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Country + year filter — used by the annual detail chart
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "#locationCode + '-' + #year")
    public List<PahoCaribCaseResponse> getByCountryAndYear(String locationCode, Integer year) {
        validateLocation(locationCode);

        return pahoRepo.findByLocationCodeAndYearOrderByEpiWeekAsc(locationCode, year)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Regional queries (all countries) ──────────────────────────────────────

    // Most recent week across all 10 Caribbean countries — powers the regional overview grid
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "'latest-all'")
    public List<PahoCaribCaseResponse> getLatestAllCountries() {
        return pahoRepo.findMostRecentWeekAllCountries()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Annual totals for all countries in a given year — powers the bar chart
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "'annual-' + #year")
    public List<Object[]> getAnnualTotalsByCountry(Integer year) {
        return pahoRepo.findAnnualCaseTotalsByCountry(year);
    }

    // Available years in the dataset — for the year-selector dropdown
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "'years'")
    public List<Integer> getAvailableYears() {
        return pahoRepo.findDistinctYears();
    }

    // Summary: total cases for Jamaica (used on the surveillance dashboard KPI)
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "'jamaica-total'")
    public Long getJamaicaTotalCases() {
        return pahoRepo.sumDengueCasesByLocation("jm");
    }

    // Latest recorded incidence rate for Jamaica — the live KPI number
    @Cacheable(value = CacheConfig.PAHO_CARIBBEAN, key = "'jamaica-latest'")
    public PahoCaribCaseResponse getJamaicaLatest() {
        PahoCaribCase latest = pahoRepo.findFirstByLocationCodeOrderByEpiWeekDesc("jm");
        if (latest == null) {
            throw new ResourceNotFoundException("No PAHO data found for Jamaica. " +
                    "Run the ETL job via POST /api/paho/etl/refresh to load data.");
        }
        return toResponse(latest);
    }

    // How many rows do we have for each country? — for the data coverage dashboard
    public List<Object[]> getCountryCoverage() {
        return pahoRepo.findDistinctLocationCodes()
                .stream()
                .map(code -> (Object[]) new Object[]{
                        code,
                        pahoRepo.countByLocationCode(code)
                })
                .collect(Collectors.toList());
    }

    // ── ETL trigger ───────────────────────────────────────────────────────────

    // LEARNING NOTE — @Transactional on a write method overrides the class-level readOnly:
    //   The class says readOnly = true. This method writes to the DB (via ETL job),
    //   so we need a full read-write transaction. @Transactional here overrides the class default.
    //
    // LEARNING NOTE — @CacheEvict when data changes:
    //   After the ETL runs and new rows are in the DB, the Caffeine cache holds stale data.
    //   @CacheEvict(value = CacheConfig.PAHO_CARIBBEAN, allEntries = true) clears ALL
    //   entries in the pahoCaribbean cache so the next request queries the fresh DB state.
    //   Without this, the cache would serve old data for up to 300 seconds after a refresh.
    @Transactional
    @CacheEvict(value = CacheConfig.PAHO_CARIBBEAN, allEntries = true)
    public int triggerRefresh() {
        log.info("PahoService: manual ETL refresh triggered — fetching new Caribbean epiweeks");
        int inserted = pahoEtlJob.refreshAllCountries();
        log.info("PahoService: refresh complete — {} new rows inserted", inserted);
        return inserted;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    // LEARNING NOTE — Entity-to-DTO mapping inline vs MapStruct:
    //   MapStruct generates the mapping code at compile time (see DengueMapper).
    //   For small, simple entities like this one, an inline helper method is equally
    //   readable and requires no extra interface + annotation setup.
    //   MapStruct is worth the boilerplate when you have many DTOs or complex field
    //   transformations. For a 12-field entity with no transformations, inline is fine.
    private PahoCaribCaseResponse toResponse(PahoCaribCase e) {
        return new PahoCaribCaseResponse(
                e.getId(),
                e.getLocationCode(),
                e.getCountryName(),
                e.getEpiWeek(),
                e.getYear(),
                e.getWeekOfYear(),
                e.getTotalPopulation(),
                e.getDengueCases(),
                e.getIncidenceRate(),
                e.getSerotype(),
                e.getDataSource(),
                e.getIngestedAt()
        );
    }

    // Input validation — throws before any DB query if the location code is invalid
    private void validateLocation(String locationCode) {
        if (locationCode == null || !VALID_LOCATIONS.contains(locationCode.toLowerCase())) {
            throw new ResourceNotFoundException(
                    "Invalid Caribbean location code: '" + locationCode + "'. " +
                    "Valid codes: jm, tt, bb, cu, do, ht, pr, bs, gy, bz"
            );
        }
    }
}
