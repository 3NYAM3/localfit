package com.localfit.domain.rent.service;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.config.RentSyncProperties;
import com.localfit.domain.rent.dto.RentStatsProjection;
import com.localfit.domain.rent.dto.RentStatsResponse;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 특정 지역의 전월세 상세 통계를 제공하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RentStatsService {

    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;
    private final RentSyncProperties properties;

    /**
     * 특정 지역의 최근 N개월 전월세 통계를 반환
     * 전/월세를 분리 집계하며, 거래 건수와 평균 금액을 함께 제공
     *
     * @param regionId 조회 대상 지역Id
     * @return 전세/월세 거래 건수. 평균보증금, 평균월세액
     */
    @Transactional(readOnly = true)
    public RentStatsResponse getStats(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        YearMonth now = YearMonth.now();
        LocalDate periodStart = now.minusMonths(properties.getMonthsToCollect() - 1L).atDay(1);
        LocalDate periodEnd = now.atEndOfMonth();

        RentStatsProjection stats = rentTransactionRepository.findStatsByRegion(regionId, periodStart, periodEnd);

        return new RentStatsResponse(
                region.getId(),
                region.getSido(),
                region.getSigungu(),
                region.getDong(),
                periodStart,
                periodEnd,
                new RentStatsResponse.JeonseStats(nullSafe(stats.getJeonseCount()), stats.getJeonseAvgDeposit()),
                new RentStatsResponse.MonthlyStats(nullSafe(stats.getMonthlyCount()), stats.getMonthlyAvgDeposit(), stats.getMonthlyAvgRent())

        );
    }

    /** null인 Long 값을 0으로 변환한다. 거래 건수가 없는 지역의 null 방어용 */
    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

}
