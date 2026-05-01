package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.DataQualityFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataQualityFlagRepository extends JpaRepository<DataQualityFlag, Integer> {

    List<DataQualityFlag> findByTableNameAndResolved(String tableName, Boolean resolved);

    List<DataQualityFlag> findByTableNameAndRowId(String tableName, Integer rowId);

    // Unresolved flag count per table — used by admin dashboard
    @Query("""
            SELECT d.tableName, COUNT(d)
            FROM DataQualityFlag d
            WHERE d.resolved = FALSE
            GROUP BY d.tableName
            ORDER BY COUNT(d) DESC
            """)
    List<Object[]> countUnresolvedByTable();

    long countByResolvedFalse();
}
