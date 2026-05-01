package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: mr_prog_alerts table (V5)
// Populated by ORANGE @Scheduled monitoring jobs
// Served by BLUE controller to the Mr. Prog widget
// ================================================

import com.arbosentinel.blue.entity.enums.AlertStatus;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mr_prog_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MrProgAlert {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // null = platform-wide alert (not disease-specific)
    @Enumerated(EnumType.STRING)
    @Column(name = "disease_type", columnDefinition = "disease_type")
    private DiseaseType diseaseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, columnDefinition = "alert_status")
    private AlertStatus alertStatus;

    @Column(name = "alert_message", nullable = false, columnDefinition = "text")
    private String alertMessage;

    @Column(name = "region_name", length = 200)
    private String regionName;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    // FALSE = dismissed or superseded — only TRUE alerts are served to frontend
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
