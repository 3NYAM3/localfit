package com.localfit.domain.recommendation.controller;

import com.localfit.domain.recommendation.dto.FavoriteRegionScoreResponse;
import com.localfit.domain.recommendation.dto.RecommendationRequest;
import com.localfit.domain.recommendation.dto.RecommendationResponse;
import com.localfit.domain.recommendation.dto.RentType;
import com.localfit.domain.recommendation.service.FavoriteRegionScoreService;
import com.localfit.domain.recommendation.service.RecommendationService;
import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final FavoriteRegionScoreService favoriteRegionScoreService;

    @PostMapping("/api/recommendations")
    public ApiResponse<RecommendationResponse> recommend(
            @Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.ok(recommendationService.recommend(request));
    }

    @GetMapping("/api/favorites/scores")
    public ApiResponse<List<FavoriteRegionScoreResponse>> getFavoriteScores(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "JEONSE") RentType rentType) {
        return ApiResponse.ok(favoriteRegionScoreService.getScores(userDetails.getUserId(), rentType));
    }
}
