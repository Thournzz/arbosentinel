package com.arbosentinel.orange;

// ================================================
// ORANGE layer — Zika ETL job
// Source: CDC 2016 Zika outbreak dataset (via Kaggle)
// File: zika.csv
// ================================================

import com.arbosentinel.blue.entity.IngestionLog;
import com.arbosentinel.blue.entity.ZikaCase;
import com.arbosentinel.blue.entity.enums.IngestionStatus;
import com.arbosentinel.purple.*;
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
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZikaEtlJob {

    private final ZikaCaseRepository zikaRepo;
    private final ArboDataSourceRepository dataSourceRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Value("${arbosentinel.data.dir}")
    private String dataDir;

    @Value("${arbosentinel.data.zika:zika.csv}")
    private String zikaFile;

    @Value("${arbosentinel.etl.batch-size:500}")
    private int batchSize;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        var dataSource = dataSourceRepo.findBySourceName("CDC Zika 2016");
        if (dataSource.isPresent()) {
            var lastRun = ingestionLogRepo.findTopByDataSourceIdOrderByRunAtDesc(dataSource.get().getId());
            if (lastRun.isPresent() && lastRun.get().getStatus() == IngestionStatus.success) {
                log.info("Zika ETL: already loaded, skipping");
                return;
            }
        }
        loadZikaData();
    }

    @Transactional
    public void loadZikaData() {
        log.info("Starting Zika ETL job");
        Integer sourceId = dataSourceRepo.findBySourceName("CDC Zika 2016")
            .map(s -> s.getId()).orElse(null);

        int processed = 0, inserted = 0, failed = 0;

        try {
            List<ZikaCase> batch = new ArrayList<>();
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, zikaFile).toFile()))) {

                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    processed++;
                    try {
                        ZikaCase record = ZikaCase.builder()
                            .reportDate(parseDate(row.get("report_date")))
                            .location(row.get("location"))
                            .locationType(row.get("location_type"))
                            .dataField(row.get("data_field"))
                            .dataFieldCode(row.get("data_field_code"))
                            .timePeriod(row.get("time_period"))
                            .timePeriodType(row.get("time_period_type"))
                            .value(parseInt(row.get("value")))
                            .unit(row.get("unit"))
                            .dataSourceId(sourceId)
                            .build();

                        batch.add(record);
                        inserted++;

                        if (batch.size() >= batchSize) {
                            zikaRepo.saveAll(batch);
                            batch.clear();
                        }
                    } catch (Exception e) {
                        failed++;
                        log.warn("Zika ETL row {}: {}", processed, e.getMessage());
                    }
                }
                if (!batch.isEmpty()) zikaRepo.saveAll(batch);
            }

            writeLog(sourceId, processed, inserted, 0, failed, IngestionStatus.success, null);
            log.info("Zika ETL complete: {} inserted", inserted);

        } catch (Exception e) {
            writeLog(sourceId, processed, inserted, 0, failed, IngestionStatus.failed, e.getMessage());
            throw new DataIngestionException("Zika ETL failed", e);
        }
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        try { return LocalDate.parse(v.trim()); } catch (Exception e) { return null; }
    }

    private void writeLog(Integer sourceId, int p, int i, int sk, int f,
                          IngestionStatus status, String error) {
        ingestionLogRepo.save(IngestionLog.builder()
            .dataSourceId(sourceId).runAt(LocalDateTime.now())
            .rowsProcessed(p).rowsInserted(i).rowsSkipped(sk).rowsFailed(f)
            .status(status).errorMessage(error).build());
    }
}
