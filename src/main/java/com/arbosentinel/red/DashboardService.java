package com.arbosentinel.red;

// ================================================
// RED layer — Dashboard aggregation service
// Assembles hero statistics from materialized views
// and cross-service totals for the landing dashboard
// ================================================

import com.arbosentinel.green.dto.DashboardStatsResponse;
import com.arbosentinel.purple.*;
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
public class DashboardService {

    private final DengueWeeklyCaseRepository dengueRepo;
    private final WestNileAnnualCaseRepository westNileRepo;
    private final MalariaEstimatedCaseRepository malariaRepo;
    private final ZikaCaseRepository zikaRepo;
    private final BrazilSinanCaseRepository sinanRepo;
    private final DataQualityFlagRepository qualityFlagRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Cacheable(CacheConfig.DASHBOARD_STATS)
    public DashboardStatsResponse getDashboardStats() {
        log.debug("Assembling dashboard statistics");

        List<DashboardStatsResponse.DiseaseTotalResponse> totals = List.of(
            buildDengueTotal(),
            buildWestNileTotal(),
            buildMalariaTotal(),
            buildZikaTotal(),
            buildChikungunyaTotal()
        );

        return new DashboardStatsResponse(
            totals,
            qualityFlagRepo.countByResolvedFalse(),
            ingestionLogRepo.countFailedRuns()
        );
    }

    // ── Per-disease total builders ───────────────────────────────

    private DashboardStatsResponse.DiseaseTotalResponse buildDengueTotal() {
        List<Object[]> totals = dengueRepo.sumTotalCasesByCity();
        long sum = totals.stream()
            .mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0L)
            .sum();
        var latestYearOpt = dengueRepo.findLatestWeekByCity().stream()
            .mapToInt(r -> (Integer) r[1])
            .max();
        return new DashboardStatsResponse.DiseaseTotalResponse(
            "dengue", sum, latestYearOpt.isPresent() ? latestYearOpt.getAsInt() : null
        );
    }

    private DashboardStatsResponse.DiseaseTotalResponse buildWestNileTotal() {
        long sum = westNileRepo.findAll().stream()
            .mapToLong(w -> w.getReportedCases() != null ? w.getReportedCases() : 0L)
            .sum();
        int latestYear = westNileRepo.findAll().stream()
            .mapToInt(w -> w.getYear())
            .max()
            .orElse(0);
        return new DashboardStatsResponse.DiseaseTotalResponse("west_nile", sum, latestYear);
    }

    private DashboardStatsResponse.DiseaseTotalResponse buildMalariaTotal() {
        List<Object[]> burden = malariaRepo.sumGlobalBurdenByYear();
        long total = burden.stream()
            .mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0L)
            .sum();
        int latestYear = burden.isEmpty() ? 0
            : (Integer) burden.get(burden.size() - 1)[0];
        return new DashboardStatsResponse.DiseaseTotalResponse("malaria", total, latestYear);
    }

    private DashboardStatsResponse.DiseaseTotalResponse buildZikaTotal() {
        List<Object[]> locationTotals = zikaRepo.sumConfirmedCasesByLocation();
        long total = locationTotals.stream()
            .mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0L)
            .sum();
        // Only return latestYear when data is actually loaded — prevents "Latest data: 2016"
        // appearing on the dashboard when the Zika ETL has not been run yet.
        Integer latestYear = total > 0 ? 2016 : null;
        return new DashboardStatsResponse.DiseaseTotalResponse("zika", total, latestYear);
    }

    private DashboardStatsResponse.DiseaseTotalResponse buildChikungunyaTotal() {
        long count = sinanRepo.count(); // total patient-level records (Brazil SINAN)
        List<Object[]> byYear = sinanRepo.countByDiseaseTypeAndYear();
        int latestYear = byYear.stream()
            .mapToInt(r -> (Integer) r[1])
            .max()
            .orElse(0);
        return new DashboardStatsResponse.DiseaseTotalResponse("chikungunya", count, latestYear);
    }
}
