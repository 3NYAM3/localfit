package com.localfit.domain.subway.dto;

import lombok.Getter;

/**
 * 지하철역 표준데이터 엑셀 파일의 행 1건 매핑 DTO
 */
@Getter
public class SubwayStationExcelRow {

    private final String stationCode;
    private final String stationName;
    private final Double latitude;
    private final Double longitude;
    private final String roadAddress;

    public SubwayStationExcelRow(String stationCode, String stationName,
                                 Double latitude, Double longitude, String roadAddress) {
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.longitude = longitude;
        this.latitude = latitude;
        this.roadAddress = roadAddress;
    }
}
