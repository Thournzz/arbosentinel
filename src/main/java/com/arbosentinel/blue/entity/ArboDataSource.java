package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: data_sources table (V1)
// Named ArboDataSource to avoid collision with javax.sql.DataSource
// ================================================

import com.arbosentinel.blue.entity.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArboDataSource {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "data_source_type")
    private DataSourceType sourceType;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "citation", columnDefinition = "text")
    private String citation;

    @Column(name = "license", length = 200)
    private String license;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
