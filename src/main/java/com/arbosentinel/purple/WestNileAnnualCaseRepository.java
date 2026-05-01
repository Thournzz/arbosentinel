package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.WestNileAnnualCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WestNileAnnualCaseRepository extends JpaRepository<WestNileAnnualCase, Integer> {

    Optional<WestNileAnnualCase> findByYear(Integer year);

    List<WestNileAnnualCase> findByYearBetweenOrderByYearAsc(Integer startYear, Integer endYear);

    // Peak year — used by risk score computation
    @Query("SELECT w FROM WestNileAnnualCase w ORDER BY w.reportedCases DESC LIMIT 1")
    Optional<WestNileAnnualCase> findPeakYear();

    // Year-over-year data ordered for trend charts
    List<WestNileAnnualCase> findAllByOrderByYearAsc();
}
