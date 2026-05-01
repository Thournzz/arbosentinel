package com.arbosentinel.orange;

// ================================================
// ORANGE layer — Dengue ETL job
// Source: DengAI / DrivenData
// Files: dengue_features_train.csv + dengue_labels_train.csv
// Strategy: Join on city+year+week_of_year at load time
// Idempotent: skips if IngestionLog shows prior success
// ================================================

import com.arbosentinel.blue.entity.DengueWeeklyCase;
import com.arbosentinel.blue.entity.IngestionLog;
import com.arbosentinel.blue.entity.enums.IngestionStatus;
import com.arbosentinel.purple.ArboDataSourceRepository;
import com.arbosentinel.purple.DengueWeeklyCaseRepository;
import com.arbosentinel.purple.IngestionLogRepository;
import com.arbosentinel.white.DataIngestionException;
import com.opencsv.CSVReaderHeaderAware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DengueEtlJob {

    private final DengueWeeklyCaseRepository dengueRepo;
    private final ArboDataSourceRepository dataSourceRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Value("${arbosentinel.data.dir}")
    private String dataDir;

    @Value("${arbosentinel.data.dengue.features:dengue_features_train.csv}")
    private String featuresFile;

    @Value("${arbosentinel.data.dengue.labels:dengue_labels_train.csv}")
    private String labelsFile;

    @Value("${arbosentinel.etl.batch-size:500}")
    private int batchSize;

    // Run once on startup — @Async so it doesn't block app start
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        // Skip if data already loaded (idempotency)
        var dataSource = dataSourceRepo.findBySourceName("DengAI - DrivenData");
        if (dataSource.isPresent()) {
            var lastRun = ingestionLogRepo.findTopByDataSourceIdOrderByRunAtDesc(dataSource.get().getId());
            if (lastRun.isPresent() && lastRun.get().getStatus() == IngestionStatus.success) {
                log.info("Dengue ETL: data already loaded ({}), skipping",
                    lastRun.get().getRunAt());
                return;
            }
        }
        loadDengueData();
    }

    @Transactional
    public void loadDengueData() {
        log.info("Starting dengue ETL job");
        Integer sourceId = dataSourceRepo.findBySourceName("DengAI - DrivenData")
            .map(s -> s.getId()).orElse(null);

        Path featuresPath = Paths.get(dataDir, featuresFile);
        Path labelsPath   = Paths.get(dataDir, labelsFile);

        int processed = 0, inserted = 0, skipped = 0, failed = 0;

        try {
            // Load labels into a lookup map: city+year+week → total_cases
            Map<String, Integer> labelsMap = loadLabelsMap(labelsPath.toString());
            log.info("Loaded {} dengue label records", labelsMap.size());

            List<DengueWeeklyCase> batch = new ArrayList<>();

            try (CSVReaderHeaderAware reader =
                     new CSVReaderHeaderAware(new FileReader(featuresPath.toFile()))) {

                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    processed++;
                    try {
                        String city = row.get("city");
                        Integer year = parseInt(row.get("year"));
                        Integer week = parseInt(row.get("weekofyear"));

                        // Skip if already in DB
                        if (dengueRepo.findByCityAndYearAndWeekOfYear(city, year, week).isPresent()) {
                            skipped++;
                            continue;
                        }

                        String labelKey = city + "_" + year + "_" + week;
                        Integer totalCases = labelsMap.get(labelKey);

                        DengueWeeklyCase record = DengueWeeklyCase.builder()
                            .city(city)
                            .year(year)
                            .weekOfYear(week)
                            .weekStartDate(parseDate(row.get("week_start_date")))
                            .totalCases(totalCases)
                            .ndviNe(parseBig(row.get("ndvi_ne")))
                            .ndviNw(parseBig(row.get("ndvi_nw")))
                            .ndviSe(parseBig(row.get("ndvi_se")))
                            .ndviSw(parseBig(row.get("ndvi_sw")))
                            .precipitationAmtMm(parseBig(row.get("precipitation_amt_mm")))
                            .reanalysisAirTempK(parseBig(row.get("reanalysis_air_temp_k")))
                            .reanalysisAvgTempK(parseBig(row.get("reanalysis_avg_temp_k")))
                            .reanalysisDewPointTempK(parseBig(row.get("reanalysis_dew_point_temp_k")))
                            .reanalysisMaxAirTempK(parseBig(row.get("reanalysis_max_air_temp_k")))
                            .reanalysisMinAirTempK(parseBig(row.get("reanalysis_min_air_temp_k")))
                            .reanalysisPrecipAmtKgPerM2(parseBig(row.get("reanalysis_precip_amt_kg_per_m2")))
                            .reanalysisRelativeHumidityPct(parseBig(row.get("reanalysis_relative_humidity_percent")))
                            .reanalysisSatPrecipAmtMm(parseBig(row.get("reanalysis_sat_precip_amt_mm")))
                            .reanalysisSpecificHumidityGKg(parseBig(row.get("reanalysis_specific_humidity_g_per_kg")))
                            .reanalysisTdtrK(parseBig(row.get("reanalysis_tdtr_k")))
                            .stationAvgTempC(parseBig(row.get("station_avg_temp_c")))
                            .stationDiurTempRngC(parseBig(row.get("station_diur_temp_rng_c")))
                            .stationMaxTempC(parseBig(row.get("station_max_temp_c")))
                            .stationMinTempC(parseBig(row.get("station_min_temp_c")))
                            .stationPrecipMm(parseBig(row.get("station_precip_mm")))
                            .dataSourceId(sourceId)
                            .build();

                        batch.add(record);
                        inserted++;

                        if (batch.size() >= batchSize) {
                            dengueRepo.saveAll(batch);
                            batch.clear();
                        }

                    } catch (Exception e) {
                        failed++;
                        log.warn("Dengue ETL row {}: {}", processed, e.getMessage());
                    }
                }

                if (!batch.isEmpty()) {
                    dengueRepo.saveAll(batch);
                }
            }

            writeIngestionLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.success, null);
            log.info("Dengue ETL complete: {} processed, {} inserted, {} skipped, {} failed",
                processed, inserted, skipped, failed);

        } catch (Exception e) {
            writeIngestionLog(sourceId, processed, inserted, skipped, failed,
                IngestionStatus.failed, e.getMessage());
            throw new DataIngestionException("Dengue ETL failed", e);
        }
    }

    // ── CSV helpers ──────────────────────────────────────────────

    private Map<String, Integer> loadLabelsMap(String labelsPath) throws Exception {
        Map<String, Integer> map = new HashMap<>();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(labelsPath))) {
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                String key = row.get("city") + "_" + row.get("year") + "_" + row.get("weekofyear");
                map.put(key, parseInt(row.get("total_cases")));
            }
        }
        return map;
    }

    private Integer parseInt(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseBig(String val) {
        if (val == null || val.isBlank()) return null;
        try { return new BigDecimal(val.trim()); } catch (NumberFormatException e) { return null; }
    }

    private java.time.LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try { return java.time.LocalDate.parse(val.trim()); } catch (Exception e) { return null; }
    }

    private void writeIngestionLog(Integer sourceId, int processed, int inserted,
                                   int skipped, int failed, IngestionStatus status, String error) {
        ingestionLogRepo.save(IngestionLog.builder()
            .dataSourceId(sourceId)
            .runAt(LocalDateTime.now())
            .rowsProcessed(processed)
            .rowsInserted(inserted)
            .rowsSkipped(skipped)
            .rowsFailed(failed)
            .status(status)
            .errorMessage(error)
            .build());
    }
}
