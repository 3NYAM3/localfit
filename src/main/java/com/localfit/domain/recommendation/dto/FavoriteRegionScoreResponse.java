package com.localfit.domain.recommendation.dto;

import lombok.Getter;

@Getter
public class FavoriteRegionScoreResponse {

    private final Long regionId;
    private final Integer priority;
    private final String sido;
    private final String sigungu;
    private final String dong;

    private final HousingScore housing;
    private final SubwayScore subway;


    public FavoriteRegionScoreResponse(Long regionId, Integer priority, String sido, String sigungu, String dong,
                                       HousingScore housing, SubwayScore subway) {
        this.regionId = regionId;
        this.priority = priority;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
        this.housing = housing;
        this.subway = subway;
    }

    // 주거비 지표 - 계층별 순위 + 원본 금액
    @Getter
    public static class HousingScore {
        private final Long avgAmount;
        private final long transactionCount;
        private final RankingInfo sigunguRanking;   // 시군구 내 순위 (펼쳐보기용)
        private final RankingInfo sidoRanking;      // 시도 내 순위 (기본 표시)
        private final RankingInfo capitalRanking;   // 수도권 전체 순위 (펼쳐보기용)

        public HousingScore(Long avgAmount, long transactionCount, RankingInfo sigunguRanking, RankingInfo sidoRanking, RankingInfo capitalRanking) {
            this.avgAmount = avgAmount;
            this.transactionCount = transactionCount;
            this.sigunguRanking = sigunguRanking;
            this.sidoRanking = sidoRanking;
            this.capitalRanking = capitalRanking;
        }
    }

    //지하철 접근성 지표 - 계층별 순위 + 역 개수
    @Getter
    public static class SubwayScore {
        private final long stationCount;
        private final RankingInfo sidoRanking;
        private final RankingInfo capitalRanking;

        public SubwayScore(long stationCount, RankingInfo sidoRanking, RankingInfo capitalRanking) {
            this.stationCount = stationCount;
            this.sidoRanking = sidoRanking;
            this.capitalRanking = capitalRanking;
        }
    }
}
