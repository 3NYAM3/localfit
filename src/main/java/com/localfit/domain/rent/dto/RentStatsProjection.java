package com.localfit.domain.rent.dto;

/**
 * 지역별 전월세 통계 응답.
 * 전세(보증금만)와 월세(보증금+월세)는 금액 규모가 크게 달라 분리해서 집계한다.
 */

public interface RentStatsProjection {
    Long getJeonseCount();
    Double getJeonseAvgDeposit();
    Long getMonthlyCount();
    Double getMonthlyAvgDeposit();
    Double getMonthlyAvgRent();
}
