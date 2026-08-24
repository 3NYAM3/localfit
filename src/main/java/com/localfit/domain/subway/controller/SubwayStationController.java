package com.localfit.domain.subway.controller;

import com.localfit.domain.subway.service.SubwayStationSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubwayStationController {

    private final SubwayStationSyncService subwayStationSyncService;

    @PostMapping("/internal/subway/sync")
    public ApiResponse<String> sync() {
        int count = subwayStationSyncService.sync();
        return ApiResponse.ok("지하철역 동기화 완료", count + "건 저장");
    }
}
