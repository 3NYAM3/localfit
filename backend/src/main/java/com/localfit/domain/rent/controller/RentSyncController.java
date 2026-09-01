package com.localfit.domain.rent.controller;

import com.localfit.domain.rent.client.RentTradeApiClient;
import com.localfit.domain.rent.service.RentSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전월세 실거래가 데이터 수집 트리거 API
 */
@RestController
@RequiredArgsConstructor
public class RentSyncController {

    private final RentSyncService rentSyncService;

    /**
     * 국토교통부 전월세 실거래가 API에서 수도권 최근 N개월 데이터를 수집
     *
     * @return 저장된 전월세 거래 건수
     */
    @PostMapping("/internal/rent/sync")
    public ApiResponse<String> sync() {
        int count = rentSyncService.sync();
        return ApiResponse.ok("전월세 동기화 완료", count + "건 저장");
    }
}
