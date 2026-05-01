package com.arbosentinel.orange;

// ================================================
// ORANGE layer — Malaria ETL job
// Source: WHO GHO (via Kaggle imdevskp/malaria-dataset)
// Files: estimated_numbers.csv
//        reported_numbers_of_cases_and_deaths.csv
// ================================================

import com.arbosentinel.blue.entity.IngestionLog;
import com.arbosentinel.blue.entity.MalariaEstimatedCase;
import com.arbosentinel.blue.entity.MalariaReportedCase;
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
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MalariaEtlJob {

    private final MalariaEstimatedCaseRepository estimatedRepo;
    private final MalariaReportedCaseRepository reportedRepo;
    private final ArboDataSourceRepository dataSourceRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Value("${arbosentinel.data.dir}")
    private String dataDir;

    @Value("${arbosentinel.data.malaria.estimated:estimated_numbers.csv}")
    private String estimatedFile;

    @Value("${arbosentinel.data.malaria.reported:reported_numbers_of_cases_and_deaths.csv}")
    private String reportedFile;

    @Value("${arbosentinel.etl.batch-size:500}")
    private int batchSize;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        var dataSource = dataSourceRepo.findBySourceName("WHO Malaria Estimated");
        if (dataSource.isPresent()) {
            var lastRun = ingestionLogRepo.findTopByDataSourceIdOrderByRunAtDesc(dataSource.get().getId());
            if (lastRun.isPresent() && lastRun.get().getStatus() == IngestionStatus.success) {
                log.info("Malaria ETL: already loaded, skipping");
                return;
            }
        }
        loadEstimated();
        loadReported();
    }

    @Transactional
    public void loadEstimated() {
        log.info("Starting malaria estimated ETL");
        Integer sourceId = dataSourceRepo.findBySourceName("WHO Malaria Estimated")
            .map(s -> s.getId()).orElse(null);

        int processed = 0, inserted = 0, skipped = 0, failed = 0;

        try {
            List<MalariaEstimatedCase> batch = new ArrayList<>();
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, estimatedFile).toFile()))) {

                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    processed++;
                    try {
                        String country = row.getOrDefault("Country", row.get("country"));
                        Integer year   = parseInt(row.getOrDefault("Year", row.get("year")));

                        if (estimatedRepo.findByCountryAndYear(country, year).isPresent()) {
                            skipped++; continue;
                        }

                        batch.add(MalariaEstimatedCase.builder()
                            .country(country)
                            .year(year)
                            .casesMedian(parseLong(row.get("cases_median")))
                            .casesMin(parseLong(row.get("cases_min")))
                            .casesMax(parseLong(row.get("cases_max")))
                            .deathsMedian(parseInt(row.get("deaths_median")))
                            .deathsMin(parseInt(row.get("deaths_min")))
                            .deathsMax(parseInt(row.get("deaths_max")))
                            .whoRegion(row.get("who_region"))
                            .dataSourceId(sourceId)
                            .build());
                        inserted++;

                        if (batch.size() >= batchSize) { estimatedRepo.saveAll(batch); batch.clear(); }
                    } catch (Exception e) { failed++; log.warn("Malaria estimated row {}: {}", processed, e.getMessage()); }
                }
                if (!batch.isEmpty()) estimatedRepo.saveAll(batch);
            }
            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.success, null);
            log.info("Malaria estimated ETL done: {} inserted", inserted);
        } catch (Exception e) {
            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.failed, e.getMessage());
            throw new DataIngestionException("Malaria estimated ETL failed", e);
        }
    }

    @Transactional
    public void loadReported() {
        log.info("Starting malaria reported ETL");
        Integer sourceId = dataSourceRepo.findBySourceName("WHO Malaria Reported")
            .map(s -> s.getId()).orElse(null);

        int processed = 0, inserted = 0, skipped = 0, failed = 0;

        try {
            List<MalariaReportedCase> batch = new ArrayList<>();
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, reportedFile).toFile()))) {

                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    processed++;
                    try {
                        String country = row.getOrDefault("Country", row.get("country"));
                        Integer year   = parseInt(row.getOrDefault("Year", row.get("year")));

                        if (reportedRepo.findByCountryAndYear(country, year).isPresent()) {
                            skipped++; continue;
                        }

                        batch.add(MalariaReportedCase.builder()
                            .country(country)
                            .year(year)
                            .reportedCases(parseBig(row.get("reported_cases")))
                            .reportedDeaths(parseBig(row.get("reported_deaths")))
                            .whoRegion(row.get("who_region"))
                            .dataSourceId(sourceId)
                            .build());
                        inserted++;

                        if (batch.size() >= batchSize) { reportedRepo.saveAll(batch); batch.clear(); }
                    } catch (Exception e) { failed++; }
                }
                if (!batch.isEmpty()) reportedRepo.saveAll(batch);
            }
            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.success, null);
            log.info("Malaria reported ETL done: {} inserted", inserted);
        } catch (Exception e) {
            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.failed, e.getMessage());
            throw new DataIngestionException("Malaria reported ETL failed", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return (int) Math.round(Double.parseDouble(v.trim().replace(",",""))); }
        catch (Exception e) { return null; }
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Math.round(Double.parseDouble(v.trim().replace(",",""))); }
        catch (Exception e) { return null; }
    }

    private BigDecimal parseBig(String v) {
        if (v == null || v.isBlank()) return null;
        try { return new BigDecimal(v.trim().replace(",","")); }
        catch (Exception e) { return null; }
    }

    private void writeLog(Integer sourceId, int p, int i, int sk, int f,
                          IngestionStatus status, String error) {
        ingestionLogRepo.save(IngestionLog.builder()
            .dataSourceId(sourceId).runAt(LocalDateTime.now())
            .rowsProcessed(p).rowsInserted(i).rowsSkipped(sk).rowsFailed(f)
            .status(status).errorMessage(error).build());
    }
}
