package com.arbosentinel.purple;

// ================================================
// PURPLE layer — @Repository
// Entity: DengueWeeklyCase (dengue_weekly_cases)
// Most query-intensive repository in the platform
// Powers: Surveillance dashboard, ML training pipeline,
//         trend charts, seasonal heatmaps
// ================================================

import com.arbosentinel.blue.entity.DengueWeeklyCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DengueWeeklyCaseRepository
        extends JpaRepository<DengueWeeklyCase, Integer>,
                JpaSpecificationExecutor<DengueWeeklyCase> {

    // ── Basic lookups ──────────────────────────────────────────

    Page<DengueWeeklyCase> findByCity(String city, Pageable pageable);

    List<DengueWeeklyCase> findByCityAndYearOrderByWeekOfYearAsc(String city, Integer year);

    Optional<DengueWeeklyCase> findByCityAndYearAndWeekOfYear(String city, Integer year, Integer weekOfYear);

    List<DengueWeeklyCase> findByCityAndYearBetweenOrderByYearAscWeekOfYearAsc(
            String city, Integer startYear, Integer endYear);

    // ── Aggregate queries (JPQL) ───────────────────────────────

    // Annual case totals per city — powers the year-over-year bar chart
    @Query("""
            SELECT d.city, d.year, SUM(d.totalCases)
            FROM DengueWeeklyCase d
            WHERE d.totalCases IS NOT NULL
            GROUP BY d.city, d.year
            ORDER BY d.city, d.year ASC
            """)
    List<Object[]> sumAnnualCasesByCity();

    // Weekly totals for a city and year — powers the seasonal wave chart
    @Query("""
            SELECT d.weekOfYear, d.totalCases, d.weekStartDate
            FROM DengueWeeklyCase d
            WHERE d.city = :city AND d.year = :year
            ORDER BY d.weekOfYear ASC
            """)
    List<Object[]> findWeeklyTimelineForCityAndYear(String city, Integer year);

    // Peak transmission week per city (avg across all years) — feeds ML context
    @Query("""
            SELECT d.city, d.weekOfYear, AVG(d.totalCases) AS avgCases
            FROM DengueWeeklyCase d
            WHERE d.totalCases IS NOT NULL
            GROUP BY d.city, d.weekOfYear
            ORDER BY d.city, avgCases DESC
            """)
    List<Object[]> findPeakWeeksByCity();

    // Latest available week for each city — used by ETL idempotency check
    @Query("""
            SELECT d.city, MAX(d.year), MAX(d.weekOfYear)
            FROM DengueWeeklyCase d
            GROUP BY d.city
            """)
    List<Object[]> findLatestWeekByCity();

    // Climate feature snapshot for ML input construction
    @Query("""
            SELECT d FROM DengueWeeklyCase d
            WHERE d.city = :city
            AND d.weekStartDate BETWEEN :start AND :end
            AND d.totalCases IS NOT NULL
            ORDER BY d.weekStartDate ASC
            """)
    List<DengueWeeklyCase> findClimateWindowForMl(String city, LocalDate start, LocalDate end);

    // Correlation-ready: weeks where both NDVI and cases are non-null
    @Query("""
            SELECT d FROM DengueWeeklyCase d
            WHERE d.city = :city
            AND d.ndviNe IS NOT NULL
            AND d.totalCases IS NOT NULL
            ORDER BY d.year ASC, d.weekOfYear ASC
            """)
    List<DengueWeeklyCase> findWeeksWithNdviAndCases(String city);

    // Total case count by city — for hero stat cards on dashboard
    @Query("""
            SELECT d.city, SUM(d.totalCases)
            FROM DengueWeeklyCase d
            WHERE d.totalCases IS NOT NULL
            GROUP BY d.city
            """)
    List<Object[]> sumTotalCasesByCity();
}
