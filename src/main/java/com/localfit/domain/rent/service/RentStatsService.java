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

@Slf4j
@Service
@RequiredArgsConstructor
public class RentStatsService {

    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;
    private final RentSyncProperties properties;

    @Transactional(readOnly = true)
    public RentStatsResponse getStats(Long regionId){
        Region region = regionRepository.findById(regionId)
                .orElseThrow(()->new CustomException(ErrorCode.REGION_NOT_FOUND));

        YearMonth now = YearMonth.now();
        LocalDate periodStart = now.minusMonths(properties.getMonthsToCollect()-1L).atDay(1);
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

    private long nullSafe(Long value){
        return value == null ? 0L : value;
    }

}
