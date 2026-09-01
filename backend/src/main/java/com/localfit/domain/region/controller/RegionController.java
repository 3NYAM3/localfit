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

/**
 * 지역 조회 API
 */
@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    /**
     * 시도/시군구/동 이름으로 지역을 검색
     *
     * @param sido     시도
     * @param sigungu  시군구
     * @param keyword  동 이름 검색어
     * @param pageable 페이지 정보
     * @return 조건에 맞는 지역 목록
     */
    @GetMapping("/api/regions")
    public ApiResponse<Page<RegionResponse>> getRegions(
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ApiResponse.ok(regionService.searchRegions(sido, sigungu, keyword, pageable));
    }

}
