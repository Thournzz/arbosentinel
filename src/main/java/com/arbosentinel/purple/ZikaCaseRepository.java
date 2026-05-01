package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.ZikaCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ZikaCaseRepository extends JpaRepository<ZikaCase, Integer> {

    Page<ZikaCase> findByLocation(String location, Pageable pageable);

    // Case-insensitive partial match — ILIKE equivalent via Spring Data
    List<ZikaCase> findByLocationContainingIgnoreCase(String locationFragment);

    List<ZikaCase> findByReportDateBetweenOrderByReportDateAsc(LocalDate start, LocalDate end);

    // Confirmed cases only — data_field contains 'confirmed'
    @Query("""
            SELECT z FROM ZikaCase z
            WHERE LOWER(z.dataField) LIKE '%confirmed%'
            ORDER BY z.reportDate DESC
            """)
    Page<ZikaCase> findConfirmedCases(Pageable pageable);

    // Distinct locations — for location filter dropdown
    @Query("SELECT DISTINCT z.location FROM ZikaCase z WHERE z.location IS NOT NULL ORDER BY z.location")
    List<String> findDistinctLocations();

    // Total confirmed cases by location — powers the outbreak heatmap
    @Query("""
            SELECT z.location, SUM(z.value)
            FROM ZikaCase z
            WHERE LOWER(z.dataField) LIKE '%confirmed%'
            AND z.value IS NOT NULL
            GROUP BY z.location
            ORDER BY SUM(z.value) DESC
            """)
    List<Object[]> sumConfirmedCasesByLocation();
}
