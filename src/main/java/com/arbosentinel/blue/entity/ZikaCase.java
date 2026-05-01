package com.arbosentinel.blue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "zika_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ZikaCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "location")
    private String location;

    @Column(name = "location_type")
    private String locationType;

    @Column(name = "data_field")
    private String dataField;

    @Column(name = "data_field_code")
    private String dataFieldCode;

    @Column(name = "time_period")
    private String timePeriod;

    @Column(name = "time_period_type")
    private String timePeriodType;

    @Column(name = "value")
    private Integer value;

    @Column(name = "unit")
    private String unit;

    @Column(name = "data_source_id")
    private Integer dataSourceId;

    @Column(name = "ingestion_log_id")
    private Integer ingestionLogId;
}
