package com.arbosentinel.purple;

// ══════════════════════════════════════════════════════════════════════════════
// PURPLE layer — @Repository
// Handles all database queries for the paho_caribbean_cases table.
//
// LEARNING NOTE — What a Repository is in Spring:
//   A Repository is the only layer that talks directly to the database.
//   It contains NO business logic — that belongs in the red/ @Service.
//   It contains NO HTTP code — that belongs in the blue/ @RestController.
//   Pure responsibility: "give me data from DB" / "save data to DB".
//
// LEARNING NOTE — JpaRepository<Entity, ID> explained:
//   By extending JpaRepository<PahoCaribCase, Integer> we get dozens of methods
//   for FREE without writing any code:
//     save(entity)        → INSERT or UPDATE (Hibernate decides based on id == null)
//     saveAll(list)       → batch INSERT/UPDATE — used by our ETL job
//     findById(id)        → SELECT * WHERE id = ?
//     findAll()           → SELECT * FROM paho_caribbean_cases
//     count()             → SELECT COUNT(*)
//     deleteById(id)      → DELETE WHERE id = ?
//   We only write custom methods when the free ones don't cover our use case.
//
// LEARNING NOTE — Spring Data method name conventions:
//   Spring Data JPA can generate SQL from method names automatically:
//     findByLocationCode("jm")
//       → SELECT * FROM paho_caribbean_cases WHERE location_code = 'jm'
//     findByYear(2023)
//       → SELECT * FROM paho_caribbean_cases WHERE year = 2023
//     findByLocationCodeAndYear("jm", 2023)
//       → SELECT * WHERE location_code = 'jm' AND year = 2023
//     findByEpiWeekBetween(202301, 202352)
//       → SELECT * WHERE epi_week BETWEEN 202301 AND 202352
//   The "And", "Or", "Between", "Like", "OrderBy" keywords all work.
//   This is called "derived queries" — Spring generates the JPQL at startup.
//
// LEARNING NOTE — When to use @Query instead of derived queries:
//   Derived queries get unreadable for complex logic (3+ conditions, GROUP BY,
//   aggregate functions, subqueries). For those, use @Query with JPQL or native SQL.
//   JPQL uses entity/field names (Java), native SQL uses table/column names (DB).
//   JPQL: @Query("SELECT p FROM PahoCaribCase p WHERE ...")
//   Native: @Query(value = "SELECT * FROM paho_caribbean_cases WHERE ...", nativeQuery = true)
// ══════════════════════════════════════════════════════════════════════════════

import com.arbosentinel.blue.entity.PahoCaribCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PahoCaribCaseRepository extends JpaRepository<PahoCaribCase, Integer> {

    // ── Basic lookups ──────────────────────────────────────────────────────────

    // All records for a given country — used by the Jamaica trend chart
    // Spring generates: SELECT * FROM paho_caribbean_cases WHERE location_code = ?
    List<PahoCaribCase> findByLocationCodeOrderByEpiWeekAsc(String locationCode);

    // All records for a given year across all countries
    List<PahoCaribCase> findByYearOrderByLocationCodeAscEpiWeekAsc(Integer year);

    // Single country + single year — used for annual summaries
    List<PahoCaribCase> findByLocationCodeAndYearOrderByEpiWeekAsc(String locationCode, Integer year);

    // Check if a specific country-week already exists — used for idempotency guard
    // Returns true if the row is present, false if not
    boolean existsByLocationCodeAndEpiWeek(String locationCode, Integer epiWeek);

    // ── Aggregate queries ──────────────────────────────────────────────────────

    // LEARNING NOTE — @Param("x") is MANDATORY on all @Query named parameters:
    //   When Spring compiles this code it needs to bind :locationCode in the JPQL
    //   to the Java parameter. @Param("locationCode") creates that binding.
    //   Omitting @Param causes "Could not locate named parameter" at runtime.

    // Total dengue cases for a country across all available years
    // COALESCE(SUM(...), 0) returns 0 instead of NULL when there are no rows
    @Query("""
            SELECT COALESCE(SUM(p.dengueCases), 0)
            FROM PahoCaribCase p
            WHERE p.locationCode = :locationCode
            """)
    Long sumDengueCasesByLocation(@Param("locationCode") String locationCode);

    // Total dengue cases for a country in a specific year
    @Query("""
            SELECT COALESCE(SUM(p.dengueCases), 0)
            FROM PahoCaribCase p
            WHERE p.locationCode = :locationCode
            AND p.year = :year
            """)
    Long sumDengueCasesByLocationAndYear(
            @Param("locationCode") String locationCode,
            @Param("year") Integer year
    );

    // Latest incidence rate for a country — for the live dashboard KPI tile
    // ORDER BY epi_week DESC LIMIT 1 via Spring Data's findFirst pattern
    PahoCaribCase findFirstByLocationCodeOrderByEpiWeekDesc(String locationCode);

    // Distinct years available in the table — for filter dropdowns in the UI
    @Query("SELECT DISTINCT p.year FROM PahoCaribCase p ORDER BY p.year DESC")
    List<Integer> findDistinctYears();

    // Distinct location codes — for building the country selector
    @Query("SELECT DISTINCT p.locationCode FROM PahoCaribCase p ORDER BY p.locationCode")
    List<String> findDistinctLocationCodes();

    // LEARNING NOTE — Caribbean regional summary query:
    //   This gets the most recent week's data for ALL countries simultaneously —
    //   powers the regional overview grid on the surveillance dashboard.
    //   Subquery finds max(epi_week) first; outer query selects rows at that week.
    //   We use nativeQuery=true here because correlated subqueries are easier to
    //   read in plain SQL than in JPQL for this pattern.
    @Query(value = """
            SELECT * FROM paho_caribbean_cases
            WHERE epi_week = (
                SELECT MAX(epi_week) FROM paho_caribbean_cases
            )
            ORDER BY dengue_cases DESC NULLS LAST
            """, nativeQuery = true)
    List<PahoCaribCase> findMostRecentWeekAllCountries();

    // Annual case totals per country — for the bar chart on the Dengue Intel page
    // Returns Object[] pairs: [locationCode, totalCases] for each country-year
    @Query("""
            SELECT p.locationCode, p.countryName, p.year, COALESCE(SUM(p.dengueCases), 0)
            FROM PahoCaribCase p
            WHERE p.year = :year
            GROUP BY p.locationCode, p.countryName, p.year
            ORDER BY SUM(p.dengueCases) DESC NULLS LAST
            """)
    List<Object[]> findAnnualCaseTotalsByCountry(@Param("year") Integer year);

    // Count how many rows we have for a given location — tells the ETL job
    // whether historical data was already loaded for that country
    Long countByLocationCode(String locationCode);
}
