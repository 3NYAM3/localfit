package com.localfit.domain.rent.dto;

/**
 * 전체 지역 전월세 통계 집계 결과 (추천 점수 계산용).
 */

public interface RegionRentStatsProjection {
    Long getRegionId();
    Long getTotalCount();
    Double getJeonseAvgDeposit();
    Double getMonthlyAvgDeposit();
    Double getMonthlyAvgRent();
}
