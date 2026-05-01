package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.WestNileMonthlyCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WestNileMonthlyRepository extends JpaRepository<WestNileMonthlyCase, Integer> {

    List<WestNileMonthlyCase> findByCaseTypeAndYearRangeOrderByMonth(String caseType, String yearRange);

    // Seasonal pattern — average cases per month across all year ranges
    @Query("""
            SELECT w.month, SUM(w.reportedCases)
            FROM WestNileMonthlyCase w
            WHERE w.caseType = :caseType
            GROUP BY w.month
            ORDER BY w.month
            """)
    List<Object[]> sumCasesByMonth(String caseType);
}
