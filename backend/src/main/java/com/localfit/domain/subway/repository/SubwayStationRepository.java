package com.localfit.domain.subway.repository;

import com.localfit.domain.subway.dto.SubwayCountProjection;
import com.localfit.domain.subway.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {

    /** 역번호 중복 여부 확인 */
    boolean existsByStationCode(String stationCode);

    /**
     * 수도권 전체 시군구별 지하철역 개수 집계
     *
     * @return 시군구별 지하철 역 개수 목록
     */
    @Query("""
            SELECT s.region.sido AS sido, s.region.sigungu AS sigungu, COUNT(s) AS stationCount
            FROM SubwayStation s
            GROUP BY s.region.sido, s.region.sigungu
            """)
    List<SubwayCountProjection> countAllBySigungu();

    /**
     *  특정 시도 내 시군구별 지하철역 개수 집계
     *
     * @param sido  대상 시도명
     * @return  해당 시도 내 시군구별 지하철역 개수 목록
     */
    @Query("""
            SELECT s.region.sido AS sido, s.region.sigungu AS sigungu, COUNT(s) AS stationCount
            FROM SubwayStation s
            WHERE s.region.sido = :sido
            GROUP BY s.region.sido, s.region.sigungu
            """)
    List<SubwayCountProjection> countBySido(@Param("sido") String sido);
}
