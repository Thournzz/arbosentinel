package com.arbosentinel.blue.entity;

// ================================================
// BLUE layer — @Entity
// Maps to: regions table (V2, extended in V6)
// Self-referencing parent_region_id for hierarchy:
//   continent -> country -> state -> city
// V6 additions: Jamaica, Kingston, Mona (for Dr. Sandiford's work)
// ================================================

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "regions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Region {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // ISO 3166-1 alpha-3 (e.g. "JAM" for Jamaica, "USA" for United States)
    @Column(name = "country_code", length = 3)
    private String countryCode;

    // US state code or equivalent sub-national code (e.g. "FL", "NY")
    @Column(name = "state_code", length = 5)
    private String stateCode;

    // 'continent', 'country', 'state', 'city', 'district'
    @Column(name = "region_type", nullable = false, length = 50)
    private String regionType;

    // Self-reference: stored as Integer (no @ManyToOne) for lightweight use
    @Column(name = "parent_region_id")
    private Integer parentRegionId;

    @Column(name = "lat", precision = 10, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", precision = 11, scale = 6)
    private BigDecimal lng;

    // WHO regional classification — e.g. "AMRO", "AFRO", "SEARO"
    @Column(name = "who_region", length = 100)
    private String whoRegion;
}
