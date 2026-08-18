package com.localfit.domain.rent.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.time.Month;

@Getter
public class RentStatsResponse {

    private final Long regionId;
    private final String sido;
    private final String sigungu;
    private final String dong;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final JeonseStats jeonse;
    private final MonthlyStats monthly;

    public RentStatsResponse(Long regionId, String sido, String sigungu, String dong,
                             LocalDate periodStart, LocalDate periodEnd,
                             JeonseStats jeonse, MonthlyStats monthly) {
        this.regionId = regionId;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.jeonse = jeonse;
        this.monthly = monthly;
    }

    // 전세 통계
    @Getter
    public static class JeonseStats {
        private final long count;
        private final Long avgDeposit;

        public JeonseStats(long count, Double avgDeposit) {
            this.count = count;
            this.avgDeposit = avgDeposit == null ? null : Math.round(avgDeposit);
        }
    }

    // 월세 통계
    @Getter
    public static class MonthlyStats {
        private final long count;
        private final Long avgDeposit;
        private final Long avgMonthlyRent;

        public MonthlyStats(long count, Double avgDeposit, Double avgMonthlyRent) {
            this.count = count;
            this.avgDeposit = avgDeposit == null ? null : Math.round(avgDeposit);
            this.avgMonthlyRent = avgMonthlyRent == null ? null : Math.round(avgMonthlyRent);
        }
    }


}
