package com.arbosentinel.purple;

import com.arbosentinel.blue.entity.ArboDataSource;
import com.arbosentinel.blue.entity.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArboDataSourceRepository extends JpaRepository<ArboDataSource, Integer> {

    Optional<ArboDataSource> findBySourceName(String sourceName);

    List<ArboDataSource> findBySourceType(DataSourceType sourceType);
}
