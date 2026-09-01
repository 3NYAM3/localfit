package com.localfit.domain.region.controller;

import com.localfit.domain.region.service.RegionSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 법정동코드 데이터 수집 트리거 API
 */
@RestController
@RequiredArgsConstructor
public class RegionSyncController {

    private final RegionSyncService regionSyncService;

    /**
     * 행안부 법정동코드 API에서 수도권 법정동 데이터를 수집한다.
     *
     * @return 저장된 법정동 건수
     */
    @PostMapping("/internal/regions/sync")
    public ApiResponse<String> sync() {
        int count = regionSyncService.sync();
        return ApiResponse.ok("법정동 동기화 완료", count + "건 저장");
    }
}
