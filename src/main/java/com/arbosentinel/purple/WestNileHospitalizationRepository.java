package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.WestNileHospitalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WestNileHospitalizationRepository extends JpaRepository<WestNileHospitalization, Integer> {

    Optional<WestNileHospitalization> findByYear(Integer year);

    List<WestNileHospitalization> findByYearBetweenOrderByYearAsc(Integer startYear, Integer endYear);

    List<WestNileHospitalization> findAllByOrderByYearAsc();
}
