package com.localfit.domain.rent.dto;

/**
 * 단일 지역의 전월세 상세 통계 조회 결과
 * 전세(보증금만)와 월세(보증금+월세)는 금액 규모가 크게 달라 분리해서 집계한다.
 */

public interface RentStatsProjection {
    Long getJeonseCount();

    Double getJeonseAvgDeposit();

    Long getMonthlyCount();

    Double getMonthlyAvgDeposit();

    Double getMonthlyAvgRent();
}
