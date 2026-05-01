package com.arbosentinel.orange;

// ================================================
// ORANGE layer — Brazil SINAN ETL job
// Source: Brazil SINAN via Kaggle
// File: all_arb_cid.csv
// LARGE FILE — batch processing mandatory
// Covers: dengue (A90), zika (A92.5), chikungunya (A92.0)
// ================================================

import com.arbosentinel.blue.entity.BrazilSinanCase;
import com.arbosentinel.blue.entity.IngestionLog;
import com.arbosentinel.blue.entity.enums.DiseaseType;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SinanEtlJob {

    private final BrazilSinanCaseRepository sinanRepo;
    private final ArboDataSourceRepository dataSourceRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Value("${arbosentinel.data.dir}")
    private String dataDir;

    @Value("${arbosentinel.data.sinan:all_arb_cid.csv}")
    private String sinanFile;

    @Value("${arbosentinel.etl.batch-size:500}")
    private int batchSize;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ICD-10 → DiseaseType mapping
    private static final Map<String, DiseaseType> CID_MAP = Map.of(
        "A90", DiseaseType.dengue,
        "A91", DiseaseType.dengue,
        "A92", DiseaseType.dengue,
        "A920", DiseaseType.chikungunya,
        "A925", DiseaseType.zika
    );

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        var dataSource = dataSourceRepo.findBySourceName("Brazil SINAN Arbovirus");
        if (dataSource.isPresent()) {
            var lastRun = ingestionLogRepo.findTopByDataSourceIdOrderByRunAtDesc(dataSource.get().getId());
            if (lastRun.isPresent() && lastRun.get().getStatus() == IngestionStatus.success) {
                log.info("SINAN ETL: already loaded, skipping");
                return;
            }
        }
        loadSinanData();
    }

    @Transactional
    public void loadSinanData() {
        log.info("Starting Brazil SINAN ETL (large file — batch processing active)");
        Integer sourceId = dataSourceRepo.findBySourceName("Brazil SINAN Arbovirus")
            .map(s -> s.getId()).orElse(null);

        int processed = 0, inserted = 0, skipped = 0, failed = 0;

        try {
            List<BrazilSinanCase> batch = new ArrayList<>();

            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, sinanFile).toFile()))) {

                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    processed++;

                    if (processed % 50000 == 0) {
                        log.info("SINAN ETL progress: {} processed, {} inserted", processed, inserted);
                    }

                    try {
                        String cid = row.getOrDefault("CID10", row.getOrDefault("disease_code", ""));
                        DiseaseType diseaseType = resolveDiseaseType(cid.toUpperCase().trim());
                        if (diseaseType == null) { skipped++; continue; }

                        BrazilSinanCase record = BrazilSinanCase.builder()
                            .diseaseType(diseaseType)
                            .diseaseCode(cid)
                            .classification(parseInt(row.get("CLASSI_FIN")))
                            .municipalityCode(parseLong(row.get("ID_MUNICIP")))
                            .sex(parseChar(row.get("CS_SEXO")))
                            .notificationDate(parseDate(row.get("DT_NOTIFIC")))
                            .symptomOnsetDate(parseDate(row.get("DT_SIN_PRI")))
                            .outcome(parseInt(row.get("EVOLUCAO")))
                            .stateCode(row.get("SG_UF_NOT"))
                            .municipalityName(row.get("NM_MUNICIPIO"))
                            .stateName(row.get("NM_UF"))
                            .year(parseInt(row.get("NU_ANO")))
                            .ageDays(parseInt(row.get("NU_IDADE_N")))
                            .notificationWeek(parseInt(row.get("SEM_NOT")))
                            .symptomOnsetWeek(parseInt(row.get("SEM_PRI")))
                            .dataSourceId(sourceId)
                            .build();

                        batch.add(record);
                        inserted++;

                        if (batch.size() >= batchSize) {
                            sinanRepo.saveAll(batch);
                            batch.clear();
                        }

                    } catch (Exception e) {
                        failed++;
                        if (failed <= 10) log.warn("SINAN row {}: {}", processed, e.getMessage());
                    }
                }

                if (!batch.isEmpty()) sinanRepo.saveAll(batch);
            }

            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.success, null);
            log.info("SINAN ETL complete: {} processed, {} inserted, {} skipped, {} failed",
                processed, inserted, skipped, failed);

        } catch (Exception e) {
            writeLog(sourceId, processed, inserted, skipped, failed, IngestionStatus.failed, e.getMessage());
            throw new DataIngestionException("SINAN ETL failed", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private DiseaseType resolveDiseaseType(String cid) {
        if (cid == null) return null;
        // Try exact match first, then prefix match
        if (CID_MAP.containsKey(cid)) return CID_MAP.get(cid);
        for (Map.Entry<String, DiseaseType> entry : CID_MAP.entrySet()) {
            if (cid.startsWith(entry.getKey())) return entry.getValue();
        }
        // A90-A99 range = dengue-related arboviral
        try {
            int code = Integer.parseInt(cid.replaceAll("[^0-9]", ""));
            if (code >= 90 && code <= 99) return DiseaseType.dengue;
        } catch (Exception ignored) {}
        return null;
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return null; }
    }

    private String parseChar(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim().substring(0, Math.min(1, v.trim().length()));
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        try { return LocalDate.parse(v.trim(), DATE_FMT); }
        catch (Exception e) {
            try { return LocalDate.parse(v.trim()); } catch (Exception ex) { return null; }
        }
    }

    private void writeLog(Integer sourceId, int p, int i, int sk, int f,
                          IngestionStatus status, String error) {
        ingestionLogRepo.save(IngestionLog.builder()
            .dataSourceId(sourceId).runAt(LocalDateTime.now())
            .rowsProcessed(p).rowsInserted(i).rowsSkipped(sk).rowsFailed(f)
            .status(status).errorMessage(error).build());
    }
}
