package com.localfit.domain.recommendation.dto;

import lombok.Getter;

/**
 * 특정 범위(시군구/시도/수도권 전체) 내에서의 순위 정보.
 * rank는 항상 "좋은 순서" 기준 1등부터 매겨진다.
 * (주거비는 낮을수록 1등, 지하철역은 많을수록 1등 - 지표마다 정렬 방향이 다르므로
 * 순위 계산 시점에 방향을 결정하고, 이 클래스는 이미 계산된 결과만 담는다)
 */

@Getter
public class RankingInfo {

    private final String scopeName;
    private final int totalCount;
    private final int rank;
    private final double percentile;

    public RankingInfo(String scopeName, int totalCount, int rank, double percentile){
        this.scopeName = scopeName;
        this.totalCount = totalCount;
        this.rank = rank;
        this.percentile = Math.round(percentile * 10) / 10.0;
    }
}
