package com.localfit.domain.recommendation.dto;

import lombok.Getter;

/**
 * 특정 범위(시군구/시도/수도권 전체) 내에서의 순위 정보.
 */

@Getter
public class RankingInfo {

    private final String scopeName;
    private final int totalCount;
    private final int rank;
    private final double percentile;

    private final boolean inherited;

    public RankingInfo(String scopeName, int totalCount, int rank, double percentile){
        this(scopeName, totalCount, rank, percentile, false);
    }

    public RankingInfo(String scopeName, int totalCount, int rank, double percentile, boolean inherited){
        this.scopeName = scopeName;
        this.totalCount = totalCount;
        this.rank = rank;
        this.percentile = Math.round(percentile * 10) / 10.0;
        this.inherited = inherited;
    }
}
