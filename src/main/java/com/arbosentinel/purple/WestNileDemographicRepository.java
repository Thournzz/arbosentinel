package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.WestNileDemographic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WestNileDemographicRepository extends JpaRepository<WestNileDemographic, Integer> {

    Optional<WestNileDemographic> findByAgeGroup(String ageGroup);

    // All age groups ordered for bar chart rendering
    List<WestNileDemographic> findAllByOrderByAgeGroupAsc();
}
