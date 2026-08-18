package com.localfit.domain.rent.controller;

import com.localfit.domain.rent.dto.RentStatsResponse;
import com.localfit.domain.rent.service.RentStatsService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RentStatsController {

    private final RentStatsService rentStatsService;

    //특정 지역의 최근 전월세 통계를 조회
    @GetMapping("/api/regions/{regionId}/rent-stats")
    public ApiResponse<RentStatsResponse> getRentStats(@PathVariable Long regionId){
        return ApiResponse.ok(rentStatsService.getStats(regionId));
    }
}
