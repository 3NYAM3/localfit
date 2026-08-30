package com.localfit.domain.recommendation.dto;

import lombok.Getter;

import java.util.Map;

/**
 * 관심지역 1건에 대한 계층별 평가 결과
 * 지표별로 정밀도가 달라 각 지표를 별도 내부 클래스로 분리
 */
@Getter
public class FavoriteRegionScoreResponse {

    private final Long regionId;
    private final Integer priority;
    private final String sido;
    private final String sigungu;
    private final String dong;

    private final WeightInfo weights;
    private final TierScore sigunguTier;
    private final TierScore sidoTier;
    private final TierScore capitalTier;

    public FavoriteRegionScoreResponse(Long regionId, Integer priority, String sido, String sigungu, String dong,
                                       WeightInfo weights, TierScore sigunguTier,
                                       TierScore sidoTier, TierScore capitalTier) {
        this.regionId = regionId;
        this.priority = priority;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
        this.weights = weights;
        this.sigunguTier = sigunguTier;
        this.sidoTier = sidoTier;
        this.capitalTier = capitalTier;
    }


    /** 이번 계산에 실제로 적용된 지표별 가중치 (사용자에게 계산 근거를 투명하게 보여주기 위함) */
    @Getter
    public static class WeightInfo {
        private final double housing;
        private final double subway;
        private final double hospital;

        public WeightInfo(Map<IndicatorType, Double> weights) {
            this.housing = toPercent(weights.get(IndicatorType.HOUSING));
            this.subway = toPercent(weights.get(IndicatorType.SUBWAY));
            this.hospital = toPercent(weights.get(IndicatorType.HOSPITAL));
        }

        private double toPercent(Double ratio) {
            return ratio == null ? 0 : Math.round(ratio * 1000) / 10.0;
        }
    }

    /** 특정 비교 계층(시군구/시도/수도권) 하나에서의 종합 점수와 지표별 세부 순위 */
    @Getter
    public static class TierScore {
        private final double totalScore;         // 가중합산된 종합 점수 (0~100, 높을수록 좋음)
        private final RankingInfo housingRanking;
        private final RankingInfo subwayRanking;
        private final RankingInfo hospitalRanking;

        public TierScore(double totalScore, RankingInfo housingRanking, RankingInfo subwayRanking, RankingInfo hospitalRanking) {
            this.totalScore = Math.round(totalScore * 10) / 10.0;
            this.housingRanking = housingRanking;
            this.subwayRanking = subwayRanking;
            this.hospitalRanking = hospitalRanking;
        }
    }
}
