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
     * 시군구/기간에 이미 저장된 거래들의 중복 판별 키를 한 번에 조회
     *
     * @param sigunguCode   시군구 법정동 코드 앞 5자리
     * @param start         집계 시작일
     * @param end           집계 종료일
     * @return  중복 판별용 복합키 목록
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
     * 특정 지역/기간의 전월세 통계를 집계
     *
     * @param regionId  조회 대상 지역ID
     * @param start     집계 시작일
     * @param end       집계 종료일
     * @return  전/월세 거래 건수, 평균 보증금, 평균 월세액
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
     * 수도권 전체 지역의 전월세 통계를 집계
     *
     * @param start     집계 시작일
     * @param end       집계 종료일
     * @param minCount  최소 거래건수 (표본왜곡 방지)
     * @return  지역별 전월세 통계 목록
     */
    @Query("""
            SELECT
                rt.region.id AS regionId,
                AVG(CASE WHEN rt.monthlyRent = 0 THEN rt.deposit END) AS jeonseAvgDeposit,
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

    /**
     * 특정 시도 범위로 제한한 지역별 전월세 통계를 집계
     *
     * @param sido      대상 시도명
     * @param start     집계 시작일
     * @param end       집계 종료일
     * @param minCount  최소 거래 건수 (표본 왜곡 방지)
     * @return  해당 시도 내 지역별 전월세 통계 목록
     */
    @Query("""
            SELECT
                rt.region.id AS regionId,
                AVG(CASE WHEN rt.monthlyRent = 0 THEN rt.deposit END) AS jeonseAvgDeposit,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.monthlyRent END) AS monthlyAvgRent
            FROM RentTransaction rt
            WHERE rt.dealDate BETWEEN :start AND :end
              AND rt.region.sido = :sido
            GROUP BY rt.region.id
            HAVING COUNT(rt) >= :minCount
            """)
    List<RegionRentStatsProjection> findRegionStatsBySido(
            @Param("sido") String sido,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("minCount") long minCount
    );

    /**
     * 특정 시군구 범위로 제한한 지역별 전월세 통계를 집계
     * @param sido      대상 시도명
     * @param sigungu   대상 시군구명
     * @param start     집계 시작일
     * @param end       집계 종료일
     * @param minCount  최소 거래 건수(표본 왜곡 방지)
     * @return  해당 시군구 내 지역별 전월세 통계 목록
     */
    @Query("""
            SELECT
                rt.region.id AS regionId,
                AVG(CASE WHEN rt.monthlyRent = 0 THEN rt.deposit END) AS jeonseAvgDeposit,
                AVG(CASE WHEN rt.monthlyRent > 0 THEN rt.monthlyRent END) AS monthlyAvgRent
            FROM RentTransaction rt
            WHERE rt.dealDate BETWEEN :start AND :end
              AND rt.region.sido = :sido
              AND rt.region.sigungu = :sigungu
            GROUP BY rt.region.id
            HAVING COUNT(rt) >= :minCount
            """)
    List<RegionRentStatsProjection> findRegionStatsBySigungu(
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("minCount") long minCount
    );
}