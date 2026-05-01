package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.WestNileStateCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WestNileStateCaseRepository extends JpaRepository<WestNileStateCase, Integer> {

    List<WestNileStateCase> findByStateCode(String stateCode);

    List<WestNileStateCase> findByCaseTypeAndYearRange(String caseType, String yearRange);

    // All states ordered by cases descending — powers the choropleth map
    @Query("""
            SELECT w FROM WestNileStateCase w
            WHERE w.caseType = :caseType AND w.yearRange = :yearRange
            ORDER BY w.reportedCases DESC
            """)
    List<WestNileStateCase> findByTypeAndRangeOrderByCasesDesc(String caseType, String yearRange);

    // Distinct year ranges available — for filter dropdowns
    @Query("SELECT DISTINCT w.yearRange FROM WestNileStateCase w ORDER BY w.yearRange")
    List<String> findDistinctYearRanges();
}
