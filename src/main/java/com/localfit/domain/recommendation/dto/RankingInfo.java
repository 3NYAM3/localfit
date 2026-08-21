package com.localfit.domain.recommendation.dto;

import lombok.Getter;

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
