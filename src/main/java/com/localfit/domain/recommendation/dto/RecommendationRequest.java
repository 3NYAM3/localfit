package com.localfit.domain.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지역 추천 요청 조건.
 */

@Getter
@Setter
@NoArgsConstructor
public class RecommendationRequest {

    private RentType rentType = RentType.JEONSE;

    @Min(1)
    @Max(100)
    private int limit = 10;

    private String sido;
}
