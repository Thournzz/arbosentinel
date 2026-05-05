package com.arbosentinel.orange;

// ================================================
// ORANGE layer — West Nile ETL job
// Source: CDC WNV Historic Data
// Files: WNV_Annual.csv, WNV_StateCase.csv,
//        WNV_Hospitalization.csv, WNV_Monthly.csv,
//        WNV_Demographics.csv
// ================================================

import com.arbosentinel.blue.entity.*;
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
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WestNileEtlJob {

    private final WestNileAnnualCaseRepository annualRepo;
    private final WestNileHospitalizationRepository hospRepo;
    private final WestNileStateCaseRepository stateRepo;
    private final WestNileMonthlyRepository monthlyRepo;
    private final WestNileDemographicRepository demographicRepo;
    private final ArboDataSourceRepository dataSourceRepo;
    private final IngestionLogRepository ingestionLogRepo;

    @Value("${arbosentinel.data.dir}")
    private String dataDir;

    @Value("${arbosentinel.data.wnv.annual:WNV_Annual.csv}")
    private String annualFile;

    @Value("${arbosentinel.data.wnv.state:WNV_StateCase.csv}")
    private String stateFile;

    @Value("${arbosentinel.data.wnv.hosp:WNV_Hospitalization.csv}")
    private String hospFile;

    @Value("${arbosentinel.data.wnv.monthly:WNV_Monthly.csv}")
    private String monthlyFile;

    @Value("${arbosentinel.data.wnv.demo:WNV_Demographics.csv}")
    private String demoFile;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        var dataSource = dataSourceRepo.findBySourceName("CDC West Nile Virus Data");
        if (dataSource.isPresent()) {
            var lastRun = ingestionLogRepo.findTopByDataSourceIdOrderByRunAtDesc(dataSource.get().getId());
            // Only skip if prior run was successful AND actually inserted rows
            // (guards against false-success logs written when BOM caused 0 inserts)
            if (lastRun.isPresent()
                    && lastRun.get().getStatus() == IngestionStatus.success
                    && lastRun.get().getRowsInserted() != null
                    && lastRun.get().getRowsInserted() > 0) {
                log.info("West Nile ETL: already loaded ({} rows), skipping",
                    lastRun.get().getRowsInserted());
                return;
            }
        }
        loadAnnual();
        loadHospitalizations();
        loadStateCases();
        loadMonthlyCases();
        loadDemographics();
    }

    // ── Annual cases ─────────────────────────────────────────────

    @Transactional
    public void loadAnnual() {
        log.info("Loading West Nile annual cases");
        Integer sourceId = getSourceId();
        int inserted = 0;
        try {
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, annualFile).toFile()))) {
                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    Integer year = parseInt(firstNonNull(row, "Year", "year", "YEAR"));
                    if (year == null || annualRepo.findByYear(year).isPresent()) continue;
                    annualRepo.save(WestNileAnnualCase.builder()
                        .year(year)
                        .reportedCases(parseIntRequired(firstNonNull(row, "Cases", "cases", "Total_Cases", "reported_cases", "Reported Cases")))
                        .dataSourceId(sourceId)
                        .build());
                    inserted++;
                }
            }
            log.info("West Nile annual: {} inserted", inserted);
            writeLog(sourceId, inserted, inserted, 0, 0, IngestionStatus.success, null);
        } catch (Exception e) {
            writeLog(sourceId, 0, inserted, 0, 1, IngestionStatus.partial, e.getMessage());
            log.warn("West Nile annual ETL incomplete: {}", e.getMessage());
        }
    }

    // ── Hospitalizations ─────────────────────────────────────────

    @Transactional
    public void loadHospitalizations() {
        Integer sourceId = getSourceId();
        int inserted = 0;
        try {
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, hospFile).toFile()))) {
                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    Integer year = parseInt(firstNonNull(row, "Year", "year"));
                    if (year == null || hospRepo.findByYear(year).isPresent()) continue;
                    hospRepo.save(WestNileHospitalization.builder()
                        .year(year)
                        .neuroinvasiveCases(parseInt(firstNonNull(row, "Neuroinvasive", "neuroinvasive_cases")))
                        .nonNeuroinvasiveCases(parseInt(firstNonNull(row, "Non-Neuroinvasive", "non_neuroinvasive_cases", "Non_neuroinvasive", "Non_Neuroinvasive")))
                        .dataSourceId(sourceId)
                        .build());
                    inserted++;
                }
            }
            log.info("West Nile hospitalizations: {} inserted", inserted);
        } catch (Exception e) {
            log.warn("West Nile hospitalization ETL incomplete: {}", e.getMessage());
        }
    }

    // ── State cases ──────────────────────────────────────────────

    @Transactional
    public void loadStateCases() {
        Integer sourceId = getSourceId();
        int inserted = 0;
        try {
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, stateFile).toFile()))) {
                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    // CDC export uses: Type, Year (as range string), Location (state code), Reported Cases
                    String caseType = firstNonNull(row, "Case_Type", "case_type", "CaseType", "Type");
                    String yearRange = firstNonNull(row, "Year_Range", "year_range", "YearRange", "Year");
                    String state    = firstNonNull(row, "State", "state", "State_Code", "state_code", "Location");
                    if (state != null && state.length() > 2) state = state.substring(0, 2);
                    stateRepo.save(WestNileStateCase.builder()
                        .caseType(caseType != null ? caseType : "Unknown")
                        .yearRange(yearRange != null ? yearRange : "Unknown")
                        .stateCode(state != null ? state : "XX")
                        .reportedCases(parseInt(firstNonNull(row, "Cases", "cases", "reported_cases", "Reported Cases")))
                        .dataSourceId(sourceId)
                        .build());
                    inserted++;
                }
            }
            log.info("West Nile state cases: {} inserted", inserted);
        } catch (Exception e) {
            log.warn("West Nile state ETL incomplete: {}", e.getMessage());
        }
    }

    // ── Monthly cases ────────────────────────────────────────────

    @Transactional
    public void loadMonthlyCases() {
        Integer sourceId = getSourceId();
        int inserted = 0;
        try {
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, monthlyFile).toFile()))) {
                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    monthlyRepo.save(WestNileMonthlyCase.builder()
                        .caseType(firstNonNull(row, "Case_Type", "case_type", "CaseType", "Type"))
                        .yearRange(firstNonNull(row, "Year_Range", "year_range", "YearRange", "Year"))
                        .month(firstNonNull(row, "Month", "month"))
                        .reportedCases(parseInt(firstNonNull(row, "Cases", "cases", "Reported Cases")))
                        .dataSourceId(sourceId)
                        .build());
                    inserted++;
                }
            }
            log.info("West Nile monthly: {} inserted", inserted);
        } catch (Exception e) {
            log.warn("West Nile monthly ETL incomplete: {}", e.getMessage());
        }
    }

    // ── Demographics ─────────────────────────────────────────────

    @Transactional
    public void loadDemographics() {
        Integer sourceId = getSourceId();
        int inserted = 0;
        try {
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                    new FileReader(Paths.get(dataDir, demoFile).toFile()))) {
                Map<String, String> row;
                while ((row = reader.readMap()) != null) {
                    demographicRepo.save(WestNileDemographic.builder()
                        .ageGroup(firstNonNull(row, "Age_Group", "age_group", "AgeGroup", "Age"))
                        .maleRate(parseBig(firstNonNull(row, "Male_Rate", "male_rate", "Male")))
                        .femaleRate(parseBig(firstNonNull(row, "Female_Rate", "female_rate", "Female")))
                        .dataSourceId(sourceId)
                        .build());
                    inserted++;
                }
            }
            log.info("West Nile demographics: {} inserted", inserted);
        } catch (Exception e) {
            log.warn("West Nile demographics ETL incomplete: {}", e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private Integer getSourceId() {
        return dataSourceRepo.findBySourceName("CDC West Nile Virus Data")
            .map(s -> s.getId()).orElse(null);
    }

    private String firstNonNull(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String val = row.get(key);
            if (val != null && !val.isBlank()) return val;
        }
        return null;
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return (int) Math.round(Double.parseDouble(v.trim().replace(",",""))); }
        catch (Exception e) { return null; }
    }

    private Integer parseIntRequired(String v) {
        Integer result = parseInt(v);
        return result != null ? result : 0;
    }

    private BigDecimal parseBig(String v) {
        if (v == null || v.isBlank()) return null;
        try { return new BigDecimal(v.trim()); } catch (Exception e) { return null; }
    }

    private void writeLog(Integer sourceId, int p, int i, int sk, int f,
                          IngestionStatus status, String error) {
        ingestionLogRepo.save(IngestionLog.builder()
            .dataSourceId(sourceId).runAt(LocalDateTime.now())
            .rowsProcessed(p).rowsInserted(i).rowsSkipped(sk).rowsFailed(f)
            .status(status).errorMessage(error).build());
    }
}
