package com.localfit.domain.rent.controller;

import com.localfit.domain.rent.dto.RentStatsResponse;
import com.localfit.domain.rent.service.RentStatsService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전월세 통계 조회 API.
 * 특정 지역의 최근 N개월 전세/월세 거래 건수와 평균 금액을 제공한다.
 */

@RestController
@RequiredArgsConstructor
public class RentStatsController {

    private final RentStatsService rentStatsService;

    /**
     * 특정 지역의 전월세 통계를 조회한다.
     *
     * @param regionId 조회 대상 지역 ID
     * @return 전세/월세 거래 건수, 평균 보증금, 평균 월세액
     */
    @GetMapping("/api/regions/{regionId}/rent-stats")
    public ApiResponse<RentStatsResponse> getRentStats(@PathVariable Long regionId) {
        return ApiResponse.ok(rentStatsService.getStats(regionId));
    }
}
