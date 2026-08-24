package com.localfit.domain.subway.repository;

import com.localfit.domain.subway.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {
    boolean existsByStationCode(String stationCode);
}
