package com.localfit.domain.rent.repository;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.dto.RentStatsProjection;
import com.localfit.domain.rent.entity.RentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentTransactionRepository extends JpaRepository<RentTransaction, Long> {


    /**
     * 특정 시군구/기간에 이미 저장된 거래들의 중복 판별 키를 한 번에 조회한다.
     *
     * 동기화 시 거래 1건마다 exists 쿼리를 날리면 수만 번의 DB 왕복이 발생한다.
     * 시군구당 1회만 조회해 Set으로 만들어두고 메모리에서 비교하도록 변경했다.
     * (성능 개선: 약 4만 회 → 80회 쿼리로 단축)
     */
    @Query("""
            SELECT CONCAT(rt.region.id, '|', rt.aptName, '|', rt.excluUseArea, '|', rt.dealDate, '|', rt.deposit)
            FROM RentTransaction rt
            WHERE rt.dealDate BETWEEN :start AND :end
              AND rt.region.regionCode LIKE CONCAT(:sigunguCode, '%')
            """)
    List<String> findDuplicateKeys(
            @Param("sigunguCode") String sigunguCode,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    /**
     * 특정 지역/기간의 전월세 통계를 집계한다.
     *
     * 전세(월세=0)와 월세(월세>0)를 CASE 문으로 나눠 단일 쿼리로 계산한다.
     * 두 유형을 따로 조회하면 쿼리 2번이 필요하지만, 이 방식으로 1번에 처리한다.
     * CASE 조건에 맞지 않는 행은 AVG 계산 시 null로 처리되어 자동으로 제외된다.
     */
    @Query("""
            SELECT
                SUM(CASE WHEN rt.monthlyRent = 0 THEN 1 ELSE 0 END) AS jeonseCount,
                AVG(CASE WHEN rt.monthlyRent = 0 THEN rt.deposit END) AS jeonseAvgDeposit,
                SUM(CASE WHEN rt.monthlyRent > 0 THEN 1 ELSE 0 END) AS monthlyCount,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.deposit END) AS monthlyAvgDeposit,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.monthlyRent END) AS monthlyAvgRent
            FROM RentTransaction rt
            WHERE rt.region.id = :regionId
              AND rt.dealDate BETWEEN :start AND :end
            """)
    RentStatsProjection findStatsByRegion(
            @Param("regionId") Long regionId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    /**
     * 모든 지역의 전월세 통계를 한 번의 쿼리로 집계한다.
     * 추천 점수 계산 시 지역마다 개별 조회하면 수천 번의 쿼리가 발생하므로,
     * GROUP BY로 한 번에 가져와 메모리에서 정규화한다.
     */
    @Query("""
            SELECT
                rt.region.id AS regionId,
                COUNT(rt) AS totalCount,
                AVG(CASE WHEN rt.monthlyRent = 0 THEN rt.deposit END) AS jeonseAvgDeposit,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.deposit END) AS monthlyAvgDeposit,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.monthlyRent END) AS monthlyAvgRent
            FROM RentTransaction rt
            WHERE rt.dealDate BETWEEN :start AND :end
            GROUP BY rt.region.id
            HAVING COUNT(rt) >= :minCount
            """)
    List<RegionRentStatsProjection> findAllRegionStats(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("minCount") long minCount
    );
}
