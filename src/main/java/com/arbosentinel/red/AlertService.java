package com.arbosentinel.red;

// ================================================
// RED layer — Mr. Prog alert business logic
// Serves alerts to the persistent widget on all pages
// Alerts are created by ORANGE @Scheduled jobs
// and dismissed by expiry or explicit deactivation
// ================================================

import com.arbosentinel.blue.entity.MrProgAlert;
import com.arbosentinel.blue.entity.enums.AlertStatus;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.green.dto.MrProgAlertResponse;
import com.arbosentinel.green.mapper.AlertMapper;
import com.arbosentinel.purple.MrProgAlertRepository;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final MrProgAlertRepository alertRepo;
    private final AlertMapper alertMapper;

    // ── Read (public) ────────────────────────────────────────────

    @Cacheable(CacheConfig.PROG_ALERTS)
    public List<MrProgAlertResponse> getAllActiveAlerts() {
        return alertMapper.toAlertResponseList(
            alertRepo.findAllCurrentlyActive(LocalDateTime.now())
        );
    }

    public List<MrProgAlertResponse> getActiveAlertsForDisease(String diseaseType) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        return alertMapper.toAlertResponseList(
            alertRepo.findActiveByDiseaseType(type, LocalDateTime.now())
        );
    }

    public List<MrProgAlertResponse> getHighPriorityAlerts() {
        return alertMapper.toAlertResponseList(
            alertRepo.findHighPriorityAlerts(LocalDateTime.now())
        );
    }

    // ── Write (called by ORANGE scheduler) ──────────────────────

    @Transactional
    @CacheEvict(value = CacheConfig.PROG_ALERTS, allEntries = true)
    public void createAlert(DiseaseType disease, AlertStatus status,
                            String message, String region, int expiryHours) {
        MrProgAlert alert = MrProgAlert.builder()
            .diseaseType(disease)
            .alertStatus(status)
            .alertMessage(message)
            .regionName(region)
            .triggeredAt(LocalDateTime.now())
            .isActive(true)
            .expiresAt(LocalDateTime.now().plusHours(expiryHours))
            .build();
        alertRepo.save(alert);
        log.info("Mr. Prog alert created: {} [{}] — {}", disease, status, message);
    }

    @Transactional
    public void createPlatformAlert(AlertStatus status, String message, int expiryHours) {
        createAlert(null, status, message, null, expiryHours);
    }

    // ── Cleanup (called by ORANGE cleanup job) ───────────────────

    @Transactional
    @CacheEvict(value = CacheConfig.PROG_ALERTS, allEntries = true)
    public int deactivateExpiredAlerts() {
        int count = alertRepo.deactivateExpired(LocalDateTime.now());
        if (count > 0) {
            log.info("Deactivated {} expired Mr. Prog alerts", count);
        }
        return count;
    }

    // ── Alert count by status (badge) ────────────────────────────

    public List<Object[]> getActiveAlertCountByStatus() {
        return alertRepo.countActiveByStatus(LocalDateTime.now());
    }
}
