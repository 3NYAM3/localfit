package com.localfit.domain.recommendation.controller;

import com.localfit.domain.recommendation.dto.FavoriteRegionScoreResponse;
import com.localfit.domain.recommendation.dto.RentType;
import com.localfit.domain.recommendation.dto.ScoreWeightRequest;
import com.localfit.domain.recommendation.service.FavoriteRegionScoreService;
import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관심 지역 점수 조회 API
 */
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final FavoriteRegionScoreService favoriteRegionScoreService;

    /**
     * 로그인한 사용자가 등록한 관심지역들을, 지표별 중요도에 따라
     * 계층별(시군구/시도/수도권) 종합 점수와 함께 반환한다.
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param rentType    임대 유형 (기본값: JEONSE)
     * @param weightRequest   지표별 중요도 (기본값: 전 지표 NORMAL)
     * @return 관심지역별 계층별 종합 점수 목록 (우선순위 순 정렬)
     */
    @GetMapping("/api/favorites/scores")
    public ApiResponse<List<FavoriteRegionScoreResponse>> getFavoriteScores(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "JEONSE") RentType rentType,
            @ModelAttribute ScoreWeightRequest weightRequest) {
        return ApiResponse.ok(favoriteRegionScoreService.getScores(userDetails.getUserId(), rentType, weightRequest));
    }
}
