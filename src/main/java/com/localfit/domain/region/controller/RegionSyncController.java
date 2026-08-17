package com.localfit.domain.region.controller;

import com.localfit.domain.region.service.RegionSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegionSyncController {

    private final RegionSyncService regionSyncService;

    @PostMapping("/internal/regions/sync")
    public ApiResponse<String> sync() {
        int count = regionSyncService.syncTargetRegions();
        return ApiResponse.ok("법정동 동기화 완료", count + "건 저장");
    }
}
