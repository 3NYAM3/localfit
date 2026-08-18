package com.localfit.domain.recommendation.controller;

import com.localfit.domain.recommendation.dto.RecommendationRequest;
import com.localfit.domain.recommendation.dto.RecommendationResponse;
import com.localfit.domain.recommendation.service.RecommendationService;
import com.localfit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/api/recommendations")
    public ApiResponse<RecommendationResponse> recommend(
            @Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.ok(recommendationService.recommend(request));
    }
}
