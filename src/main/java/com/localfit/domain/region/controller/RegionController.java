package com.localfit.domain.region.controller;

import com.localfit.domain.region.dto.RegionResponse;
import com.localfit.domain.region.service.RegionService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/api/regions")
    public ApiResponse<Page<RegionResponse>> getRegions(
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ){
        Page<RegionResponse> result = regionService.searchRegions(sido, sigungu, keyword, pageable);
        return ApiResponse.ok(result);
    }

}
