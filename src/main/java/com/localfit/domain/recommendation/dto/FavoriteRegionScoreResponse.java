package com.localfit.domain.recommendation.dto;

import lombok.Getter;

@Getter
public class FavoriteRegionScoreResponse {

    private final Long regionId;
    private final Integer priority;
    private final String sido;
    private final String sigungu;
    private final String dong;
    private final Long avgAmount;         // 기준 금액 (전세: 보증금 / 월세: 월세액)
    private final long transactionCount;

    private final RankingInfo sigunguRanking;   // 시군구 내 순위 (펼쳐보기용)
    private final RankingInfo sidoRanking;      // 시도 내 순위 (기본 표시)
    private final RankingInfo capitalRanking;   // 수도권 전체 순위 (펼쳐보기용)


    public FavoriteRegionScoreResponse(Long regionId, Integer priority, String sido, String sigungu, String dong,
                                       Long avgAmount, long transactionCount,
                                       RankingInfo sigunguRanking, RankingInfo sidoRanking,
                                       RankingInfo capitalRanking) {
        this.regionId = regionId;
        this.priority = priority;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
        this.avgAmount = avgAmount;
        this.transactionCount = transactionCount;
        this.sigunguRanking = sigunguRanking;
        this.sidoRanking = sidoRanking;
        this.capitalRanking = capitalRanking;
    }
}
