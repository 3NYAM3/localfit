package com.localfit.domain.subway.repository;

import com.localfit.domain.subway.dto.SubwayCountProjection;
import com.localfit.domain.subway.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {
    boolean existsByStationCode(String stationCode);

    /**
     * 수도권 전체 - 시군구별 역 개수.
     * region.id가 아닌 sido+sigungu 문자열로 그룹핑한다.
     * 지하철역은 시군구 단위로만 매칭되어 저장되므로(RegionAddressMatcher 참고),
     * 여러 동이 같은 시군구를 공유하는 Region 구조와 region.id 기준으로는
     * 정확히 맞아떨어지지 않기 때문이다.
     */
    @Query("""
            SELECT s.region.sido AS sido, s.region.sigungu AS sigungu, COUNT(s) AS stationCount
            FROM SubwayStation s
            GROUP BY s.region.sido, s.region.sigungu
            """)
    List<SubwayCountProjection> countAllBySigungu();

    /** 특정 시도 내 - 시군구별 역 개수 */
    @Query("""
            SELECT s.region.sido AS sido, s.region.sigungu AS sigungu, COUNT(s) AS stationCount
            FROM SubwayStation s
            WHERE s.region.sido = :sido
            GROUP BY s.region.sido, s.region.sigungu
            """)
    List<SubwayCountProjection> countBySido(@Param("sido") String sido);
}
