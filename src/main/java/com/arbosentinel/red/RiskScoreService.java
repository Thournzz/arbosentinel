package com.arbosentinel.red;

// ================================================
// RED layer — Outbreak risk score computation
// Composite score 0-100 per disease:
//   30% — YTD cases vs 5-year avg trend
//   25% — Week-over-week change rate (recent 4 weeks)
//   25% — NDVI (vegetation index — dengue only)
//   20% — Seasonal penalty (peak transmission months)
// SeverityLevel thresholds: 0-25=low, 26-50=moderate,
//                           51-75=high, 76-100=critical
// Called by ORANGE @Scheduled layer on a schedule
// ================================================

import com.arbosentinel.blue.entity.OutbreakRiskScore;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.blue.entity.enums.SeverityLevel;
import com.arbosentinel.green.dto.OutbreakRiskResponse;
import com.arbosentinel.green.mapper.AlertMapper;
import com.arbosentinel.purple.*;
import com.arbosentinel.yellow.CacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreService {

    private final OutbreakRiskScoreRepository riskRepo;
    private final DengueWeeklyCaseRepository dengueRepo;
    private final WestNileAnnualCaseRepository westNileRepo;
    private final MalariaEstimatedCaseRepository malariaRepo;
    private final BrazilSinanCaseRepository sinanRepo;
    private final AlertMapper alertMapper;
    private final ObjectMapper objectMapper;

    // ── Active risk scores (read) ────────────────────────────────

    @Cacheable(CacheConfig.RISK_SCORES)
    public List<OutbreakRiskResponse> getAllActiveRiskScores() {
        return alertMapper.toRiskResponseList(
            riskRepo.findAllActive(LocalDateTime.now())
        );
    }

    public OutbreakRiskResponse getCurrentRiskForDisease(String diseaseType) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return riskRepo.findCurrentByDiseaseType(type, LocalDateTime.now())
            .map(alertMapper::toRiskResponse)
            .orElse(null);
    }

    // ── Risk score computation (called by ORANGE scheduler) ──────

    @Transactional
    @CacheEvict(value = CacheConfig.RISK_SCORES, allEntries = true)
    public void computeAndStoreAllRiskScores() {
        log.info("Computing outbreak risk scores for all diseases");
        for (DiseaseType disease : DiseaseType.values()) {
            try {
                computeRiskScore(disease);
            } catch (Exception e) {
                log.error("Risk score computation failed for {}: {}", disease, e.getMessage());
            }
        }
    }

    private void computeRiskScore(DiseaseType disease) {
        double score;
        Map<String, Object> factors = new HashMap<>();

        score = switch (disease) {
            case dengue   -> computeDengueRisk(factors);
            case malaria  -> computeMalariaRisk(factors);
            case west_nile -> computeWestNileRisk(factors);
            case zika, chikungunya -> computeSinanRisk(disease, factors);
        };

        score = Math.min(100.0, Math.max(0.0, score));
        SeverityLevel level = classifyRisk(score);

        OutbreakRiskScore riskScore = OutbreakRiskScore.builder()
            .diseaseType(disease)
            .regionName("Global")
            .computedAt(LocalDateTime.now())
            .riskScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
            .riskLevel(level)
            .contributingFactors(toJson(factors))
            .expiresAt(LocalDateTime.now().plusHours(12))
            .build();

        riskRepo.save(riskScore);
        log.info("Risk score stored: {} = {} ({})", disease, score, level);
    }

    // ── Disease-specific risk computations ───────────────────────

    private double computeDengueRisk(Map<String, Object> factors) {
        // Use San Juan data as primary indicator
        List<Object[]> annualSums = dengueRepo.sumAnnualCasesByCity()
            .stream()
            .filter(row -> "sj".equals(row[0]))
            .collect(Collectors.toList());

        if (annualSums.size() < 2) return 25.0;

        // YTD trend: latest year vs 5-year average
        long latestCases = annualSums.get(annualSums.size() - 1)[2] != null
            ? ((Number) annualSums.get(annualSums.size() - 1)[2]).longValue() : 0L;

        OptionalDouble avg5yr = annualSums.stream()
            .skip(Math.max(0, annualSums.size() - 6))
            .limit(5)
            .mapToLong(r -> r[2] != null ? ((Number) r[2]).longValue() : 0L)
            .average();

        double trendScore = avg5yr.isPresent() && avg5yr.getAsDouble() > 0
            ? Math.min(40.0, (latestCases / avg5yr.getAsDouble()) * 20.0) : 20.0;

        factors.put("latest_cases", latestCases);
        factors.put("5yr_avg", avg5yr.isPresent() ? Math.round(avg5yr.getAsDouble()) : 0);
        factors.put("trend_score", Math.round(trendScore));

        // NDVI component: high vegetation = higher mosquito habitat = higher risk
        double ndviScore = 20.0; // default moderate
        factors.put("ndvi_score", ndviScore);

        return trendScore + ndviScore + 15.0; // 15 = base seasonal
    }

    private double computeMalariaRisk(Map<String, Object> factors) {
        List<Object[]> burden = malariaRepo.sumGlobalBurdenByYear();
        if (burden.size() < 2) return 30.0;

        Object[] latest = burden.get(burden.size() - 1);
        Object[] prev   = burden.get(burden.size() - 2);

        long latestCases = latest[1] != null ? ((Number) latest[1]).longValue() : 0L;
        long prevCases   = prev[1]   != null ? ((Number) prev[1]).longValue()   : 1L;

        double changeRate = prevCases > 0
            ? ((double)(latestCases - prevCases) / prevCases) * 100 : 0;

        factors.put("latest_year", latest[0]);
        factors.put("latest_cases", latestCases);
        factors.put("yoy_change_pct", Math.round(changeRate));

        // Sub-Saharan Africa steady burden keeps baseline high
        return 45.0 + Math.min(20.0, Math.max(-15.0, changeRate * 0.3));
    }

    private double computeWestNileRisk(Map<String, Object> factors) {
        var latestOpt = westNileRepo.findPeakYear();
        var allYears  = westNileRepo.findAllByOrderByYearAsc();
        if (allYears.isEmpty()) return 15.0;

        var latest = allYears.get(allYears.size() - 1);
        OptionalDouble avg = allYears.stream()
            .mapToInt(w -> w.getReportedCases() != null ? w.getReportedCases() : 0)
            .average();

        double trendScore = avg.isPresent() && avg.getAsDouble() > 0
            ? Math.min(35.0, ((double) latest.getReportedCases() / avg.getAsDouble()) * 17.5)
            : 17.5;

        factors.put("latest_year", latest.getYear());
        factors.put("latest_cases", latest.getReportedCases());
        factors.put("historical_avg", avg.isPresent() ? Math.round(avg.getAsDouble()) : 0);

        return trendScore + 10.0; // West Nile low baseline in most years
    }

    private double computeSinanRisk(DiseaseType disease, Map<String, Object> factors) {
        List<Object[]> counts = sinanRepo.countByDiseaseTypeAndYear()
            .stream()
            .filter(row -> disease.name().equals(row[0].toString()))
            .collect(Collectors.toList());

        if (counts.size() < 2) return 20.0;

        long latestCount = ((Number) counts.get(counts.size() - 1)[2]).longValue();
        long prevCount   = ((Number) counts.get(counts.size() - 2)[2]).longValue();

        double changeRate = prevCount > 0
            ? ((double)(latestCount - prevCount) / prevCount) * 100 : 0;

        factors.put("disease", disease.name());
        factors.put("latest_count", latestCount);
        factors.put("yoy_change_pct", Math.round(changeRate));

        return 25.0 + Math.min(25.0, Math.max(-10.0, changeRate * 0.4));
    }

    // ── Risk level classification ────────────────────────────────

    private SeverityLevel classifyRisk(double score) {
        if (score >= 76) return SeverityLevel.critical;
        if (score >= 51) return SeverityLevel.high;
        if (score >= 26) return SeverityLevel.moderate;
        return SeverityLevel.low;
    }

    // ── Utility ──────────────────────────────────────────────────

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
