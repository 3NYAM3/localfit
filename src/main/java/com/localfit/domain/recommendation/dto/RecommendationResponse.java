package com.localfit.domain.recommendation.dto;

import lombok.Getter;

import java.util.List;

/**
 * 지역 추천 결과
 */

@Getter
public class RecommendationResponse {

    private final RentType rentType;
    private final int totalCandidates;
    private final ScoreContext scoreContext;
    private final List<RegionScore> result;

    public RecommendationResponse(RentType rentType, int totalCandidates, ScoreContext scoreContext, List<RegionScore> result){
        this.rentType = rentType;
        this.totalCandidates = totalCandidates;
        this.scoreContext = scoreContext;
        this.result = result;
    }

    @Getter
    public static class ScoreContext {
        private final String comparisonScope;   // 비교 대상 범위 (예: "수도권 전체", "서울특별시")
        private final String notice;            // 점수 해석 안내 문구
        private final long minAmount;           // 비교 풀의 최솟값 (만원)
        private final long maxAmount;           // 비교 풀의 최댓값 (만원)

        public ScoreContext(String comparisonScope, String notice, long minAmount, long maxAmount) {
            this.comparisonScope = comparisonScope;
            this.notice = notice;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }

    //지역별 점수
    @Getter
    public static class RegionScore {
        private final Long regionId;
        private final String sido;
        private final String sigungu;
        private final String dong;
        private final long transactionCount;   // 표본 거래 건수
        private final Long avgAmount;          // 기준 금액 (전세: 보증금 / 월세: 월세액)
        private final double housingScore;     // 주거비 점수 (0~100, 저렴할수록 높음)
        private final double totalScore;       // 최종 점수 (현재는 주거비 점수와 동일)

        public RegionScore(Long regionId, String sido, String sigungu, String dong,
                           long transactionCount, Long avgAmount,
                           double housingScore, double totalScore) {
            this.regionId = regionId;
            this.sido = sido;
            this.sigungu = sigungu;
            this.dong = dong;
            this.transactionCount = transactionCount;
            this.avgAmount = avgAmount;
            this.housingScore = Math.round(housingScore * 10) / 10.0;   // 소수점 1자리
            this.totalScore = Math.round(totalScore * 10) / 10.0;
        }
    }
}
