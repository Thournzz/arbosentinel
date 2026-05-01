package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: dengue_weekly_cases table (V3)
// Source: DengAI / DrivenData
// Powers: Surveillance dashboard + ML training
// ================================================

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dengue_weekly_cases")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DengueWeeklyCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "city", nullable = false)
    private String city;  // 'sj' = San Juan, 'iq' = Iquitos

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "week_of_year", nullable = false)
    private Integer weekOfYear;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "total_cases")
    private Integer totalCases;

    // NDVI — vegetation index (mosquito habitat proxy)
    @Column(name = "ndvi_ne", precision = 10, scale = 7)
    private BigDecimal ndviNe;

    @Column(name = "ndvi_nw", precision = 10, scale = 7)
    private BigDecimal ndviNw;

    @Column(name = "ndvi_se", precision = 10, scale = 7)
    private BigDecimal ndviSe;

    @Column(name = "ndvi_sw", precision = 10, scale = 7)
    private BigDecimal ndviSw;

    @Column(name = "precipitation_amt_mm", precision = 10, scale = 4)
    private BigDecimal precipitationAmtMm;

    @Column(name = "reanalysis_air_temp_k", precision = 12, scale = 6)
    private BigDecimal reanalysisAirTempK;

    @Column(name = "reanalysis_avg_temp_k", precision = 12, scale = 6)
    private BigDecimal reanalysisAvgTempK;

    @Column(name = "reanalysis_dew_point_temp_k", precision = 12, scale = 6)
    private BigDecimal reanalysisDewPointTempK;

    @Column(name = "reanalysis_max_air_temp_k", precision = 12, scale = 6)
    private BigDecimal reanalysisMaxAirTempK;

    @Column(name = "reanalysis_min_air_temp_k", precision = 12, scale = 6)
    private BigDecimal reanalysisMinAirTempK;

    @Column(name = "reanalysis_precip_amt_kg_per_m2", precision = 12, scale = 6)
    private BigDecimal reanalysisPrecipAmtKgPerM2;

    @Column(name = "reanalysis_relative_humidity_pct", precision = 10, scale = 6)
    private BigDecimal reanalysisRelativeHumidityPct;

    @Column(name = "reanalysis_sat_precip_amt_mm", precision = 12, scale = 6)
    private BigDecimal reanalysisSatPrecipAmtMm;

    @Column(name = "reanalysis_specific_humidity_g_kg", precision = 12, scale = 6)
    private BigDecimal reanalysisSpecificHumidityGKg;

    @Column(name = "reanalysis_tdtr_k", precision = 12, scale = 6)
    private BigDecimal reanalysisTdtrK;

    @Column(name = "station_avg_temp_c", precision = 10, scale = 6)
    private BigDecimal stationAvgTempC;

    @Column(name = "station_diur_temp_rng_c", precision = 10, scale = 6)
    private BigDecimal stationDiurTempRngC;

    @Column(name = "station_max_temp_c", precision = 10, scale = 4)
    private BigDecimal stationMaxTempC;

    @Column(name = "station_min_temp_c", precision = 10, scale = 4)
    private BigDecimal stationMinTempC;

    @Column(name = "station_precip_mm", precision = 10, scale = 4)
    private BigDecimal stationPrecipMm;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
