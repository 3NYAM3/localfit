package com.localfit.domain.rent.repository;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.rent.dto.RentStatsProjection;
import com.localfit.domain.rent.entity.RentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentTransactionRepository extends JpaRepository<RentTransaction, Long> {

    boolean existsByRegionAndAptNameAndExcluUseAreaAndDealDateAndDeposit(
            Region region, String aptName, Double excluUseArea, LocalDate dealDate, Long deposit);

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
}
