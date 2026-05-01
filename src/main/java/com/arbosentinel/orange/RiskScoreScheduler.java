package com.arbosentinel.orange;

// ================================================
// ORANGE layer — Risk score + Mr. Prog alert scheduler
// Runs on schedule:
//   - Risk score computation: every 12 hours
//   - Alert generation based on new risk scores: every 12 hours
//   - Alert cleanup: every 6 hours
//   - Dashboard cache refresh: every hour
// ================================================

import com.arbosentinel.blue.entity.enums.AlertStatus;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.green.dto.OutbreakRiskResponse;
import com.arbosentinel.red.AlertService;
import com.arbosentinel.red.RiskScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskScoreScheduler {

    private final RiskScoreService riskScoreService;
    private final AlertService alertService;

    // ── Risk score computation ───────────────────────────────────
    // Every 12 hours at :00
    @Scheduled(cron = "0 0 0,12 * * *")
    public void computeRiskScores() {
        log.info("Scheduled risk score computation triggered");
        riskScoreService.computeAndStoreAllRiskScores();
        generateAlertsFromRiskScores();
    }

    // ── Alert generation based on risk scores ────────────────────
    public void generateAlertsFromRiskScores() {
        List<OutbreakRiskResponse> activeScores = riskScoreService.getAllActiveRiskScores();

        for (OutbreakRiskResponse score : activeScores) {
            if (score.riskLevel() == null) continue;

            DiseaseType disease = DiseaseType.valueOf(score.diseaseType());
            String riskLevel    = score.riskLevel();

            switch (riskLevel) {
                case "critical" -> alertService.createAlert(
                    disease, AlertStatus.critical,
                    buildAlertMessage(score.diseaseType(), riskLevel, score.riskScore().doubleValue()),
                    score.regionName(), 24
                );
                case "high" -> alertService.createAlert(
                    disease, AlertStatus.high,
                    buildAlertMessage(score.diseaseType(), riskLevel, score.riskScore().doubleValue()),
                    score.regionName(), 24
                );
                case "moderate" -> alertService.createAlert(
                    disease, AlertStatus.moderate,
                    buildAlertMessage(score.diseaseType(), riskLevel, score.riskScore().doubleValue()),
                    score.regionName(), 48
                );
                // low risk — no alert needed
                default -> {}
            }
        }
    }

    // ── Alert cleanup ────────────────────────────────────────────
    // Every 6 hours
    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredAlerts() {
        int deactivated = alertService.deactivateExpiredAlerts();
        log.debug("Alert cleanup: {} expired alerts deactivated", deactivated);
    }

    // ── Dashboard cache eviction ─────────────────────────────────
    // Every hour — forces fresh stats on next request
    @Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void refreshDashboardCache() {
        log.debug("Dashboard stats cache evicted");
    }

    // ── Message builder ──────────────────────────────────────────

    private String buildAlertMessage(String disease, String level, double score) {
        String displayName = switch (disease) {
            case "dengue"      -> "Dengue";
            case "malaria"     -> "Malaria";
            case "zika"        -> "Zika";
            case "west_nile"   -> "West Nile virus";
            case "chikungunya" -> "Chikungunya";
            default            -> disease;
        };

        return String.format(
            "%s outbreak risk elevated to %s (score: %.0f/100). " +
            "Monitor surveillance data and review prevention guidelines.",
            displayName, level.toUpperCase(), score
        );
    }
}
