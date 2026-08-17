package com.localfit.domain.rent.controller;

import com.localfit.domain.rent.client.RentTradeApiClient;
import com.localfit.domain.rent.dto.RentTradeApiResponse;
import com.localfit.domain.rent.service.RentSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RentSyncController {

    private final RentSyncService rentSyncService;
    private final RentTradeApiClient rentTradeApiClient;

    @PostMapping("/internal/rent/sync")
    public ApiResponse<String> sync() {
        int count = rentSyncService.syncRecentMonths();
        return ApiResponse.ok("전월세 동기화 완료", count + "건 저장");
    }

    @GetMapping("/internal/rent/debug")
    public String debug() {
        RentTradeApiResponse response = rentTradeApiClient.fetch("11110", "202601", 1, 10);
        return "resultCode=" + response.getResultCode() + ", items=" + response.getItems().size();
    }
}
