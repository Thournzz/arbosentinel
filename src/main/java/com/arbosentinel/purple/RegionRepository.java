package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Integer> {

    Optional<Region> findByNameAndCountryCode(String name, String countryCode);

    List<Region> findByCountryCode(String countryCode);

    List<Region> findByRegionType(String regionType);

    List<Region> findByWhoRegion(String whoRegion);

    // Child regions of a parent — for hierarchical map drill-down
    List<Region> findByParentRegionId(Integer parentRegionId);

    // All countries with lat/lng — for map initialization
    @Query("SELECT r FROM Region r WHERE r.regionType = 'country' AND r.lat IS NOT NULL ORDER BY r.name")
    List<Region> findAllCountriesWithCoordinates();
}
