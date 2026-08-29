package com.localfit.domain.subway.dto;

/**
 * 시군구 단위 지하철역 개수 집계 결과
 */
public interface SubwayCountProjection {
    String getSido();

    String getSigungu();

    Long getStationCount();
}
