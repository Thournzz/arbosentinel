package com.arbosentinel.purple;

// ================================================
// PURPLE layer — @Repository
// Entity: MrProgAlert (mr_prog_alerts)
// Queried by ORANGE scheduler + BLUE controller
// Key rule: only isActive=TRUE and not-expired are served
// ================================================

import com.arbosentinel.blue.entity.MrProgAlert;
import com.arbosentinel.blue.entity.enums.AlertStatus;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MrProgAlertRepository extends JpaRepository<MrProgAlert, Integer> {

    // All alerts currently active and not expired — primary query for widget
    @Query("""
            SELECT m FROM MrProgAlert m
            WHERE m.isActive = TRUE
            AND (m.expiresAt IS NULL OR m.expiresAt > :now)
            ORDER BY m.triggeredAt DESC
            """)
    List<MrProgAlert> findAllCurrentlyActive(LocalDateTime now);

    // Active alerts for a specific disease — for disease detail pages
    @Query("""
            SELECT m FROM MrProgAlert m
            WHERE m.diseaseType = :diseaseType
            AND m.isActive = TRUE
            AND (m.expiresAt IS NULL OR m.expiresAt > :now)
            ORDER BY m.triggeredAt DESC
            """)
    List<MrProgAlert> findActiveByDiseaseType(DiseaseType diseaseType, LocalDateTime now);

    // Critical and high alerts only — for emergency notification banner
    @Query("""
            SELECT m FROM MrProgAlert m
            WHERE m.alertStatus IN ('high', 'critical')
            AND m.isActive = TRUE
            AND (m.expiresAt IS NULL OR m.expiresAt > :now)
            ORDER BY m.alertStatus DESC, m.triggeredAt DESC
            """)
    List<MrProgAlert> findHighPriorityAlerts(LocalDateTime now);

    // Deactivate all expired alerts — called by @Scheduled cleanup job (ORANGE layer)
    @Modifying
    @Transactional
    @Query("""
            UPDATE MrProgAlert m SET m.isActive = FALSE
            WHERE m.expiresAt IS NOT NULL AND m.expiresAt <= :now AND m.isActive = TRUE
            """)
    int deactivateExpired(LocalDateTime now);

    // Count active alerts by status — for Mr. Prog badge count
    @Query("""
            SELECT m.alertStatus, COUNT(m)
            FROM MrProgAlert m
            WHERE m.isActive = TRUE
            AND (m.expiresAt IS NULL OR m.expiresAt > :now)
            GROUP BY m.alertStatus
            """)
    List<Object[]> countActiveByStatus(LocalDateTime now);

    List<MrProgAlert> findByAlertStatus(AlertStatus alertStatus);
}
