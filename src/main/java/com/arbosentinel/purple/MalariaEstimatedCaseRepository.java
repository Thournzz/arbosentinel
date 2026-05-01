package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.MalariaEstimatedCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MalariaEstimatedCaseRepository extends JpaRepository<MalariaEstimatedCase, Integer> {

    Optional<MalariaEstimatedCase> findByCountryAndYear(String country, Integer year);

    Page<MalariaEstimatedCase> findByCountry(String country, Pageable pageable);

    List<MalariaEstimatedCase> findByCountryOrderByYearAsc(String country);

    List<MalariaEstimatedCase> findByWhoRegionAndYearOrderByCasesMedianDesc(String whoRegion, Integer year);

    Page<MalariaEstimatedCase> findByYear(Integer year, Pageable pageable);

    // Global total burden per year — powers the timeline chart
    @Query("""
            SELECT m.year, SUM(m.casesMedian), SUM(m.deathsMedian)
            FROM MalariaEstimatedCase m
            GROUP BY m.year
            ORDER BY m.year ASC
            """)
    List<Object[]> sumGlobalBurdenByYear();

    // Top N countries by case burden for a given year
    @Query("""
            SELECT m FROM MalariaEstimatedCase m
            WHERE m.year = :year AND m.casesMedian IS NOT NULL
            ORDER BY m.casesMedian DESC
            """)
    List<MalariaEstimatedCase> findTopCountriesByYear(Integer year, Pageable pageable);

    // WHO region summary — feeds the region breakdown chart
    @Query("""
            SELECT m.whoRegion, m.year, SUM(m.casesMedian), SUM(m.deathsMedian)
            FROM MalariaEstimatedCase m
            WHERE m.whoRegion IS NOT NULL
            GROUP BY m.whoRegion, m.year
            ORDER BY m.whoRegion, m.year
            """)
    List<Object[]> sumByWhoRegionAndYear();
}
