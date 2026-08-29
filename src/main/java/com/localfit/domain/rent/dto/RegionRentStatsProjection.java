package com.localfit.domain.rent.dto;

/**
 * 계층별 주거비 순위 계산을 위한 지역별 전월세 통계 집계 결과
 */

public interface RegionRentStatsProjection {
    Long getRegionId();

    Double getJeonseAvgDeposit();

    Double getMonthlyAvgRent();
}
