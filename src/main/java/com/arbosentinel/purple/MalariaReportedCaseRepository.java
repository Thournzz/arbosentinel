package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.MalariaReportedCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MalariaReportedCaseRepository extends JpaRepository<MalariaReportedCase, Integer> {

    Optional<MalariaReportedCase> findByCountryAndYear(String country, Integer year);

    List<MalariaReportedCase> findByCountryOrderByYearAsc(String country);

    Page<MalariaReportedCase> findByYear(Integer year, Pageable pageable);

    List<MalariaReportedCase> findByWhoRegionAndYear(String whoRegion, Integer year);

    // Compare estimated vs reported — pairs with MalariaEstimatedCaseRepository
    @Query("""
            SELECT r.country, r.year, r.reportedCases, r.reportedDeaths
            FROM MalariaReportedCase r
            WHERE r.year = :year AND r.reportedCases IS NOT NULL
            ORDER BY r.reportedCases DESC
            """)
    List<Object[]> findReportedByYearOrdered(Integer year, Pageable pageable);
}
