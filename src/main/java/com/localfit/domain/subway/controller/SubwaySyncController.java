package com.localfit.domain.subway.controller;

import com.localfit.domain.subway.service.SubwayStationSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지하철역 데이터 수집 트리거 API
 */
@RestController
@RequiredArgsConstructor
public class SubwaySyncController {

    private final SubwayStationSyncService subwayStationSyncService;

    /**
     * 리소스 파일에서 지하철역 데이터를 읽어 수도권 Region과 매칭해 저장한다.
     *
     * @return 저장된 지하철역 건수
     */
    @PostMapping("/internal/subway/sync")
    public ApiResponse<String> sync() {
        int count = subwayStationSyncService.sync();
        return ApiResponse.ok("지하철역 동기화 완료", count + "건 저장");
    }
}
