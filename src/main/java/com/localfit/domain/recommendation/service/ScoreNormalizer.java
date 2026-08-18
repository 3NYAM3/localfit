package com.localfit.domain.recommendation.service;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 지표별로 단위가 다른 원본 값(보증금 수억원, 정류장 수십 개 등)을
 * 0~100 척도로 변환하는 정규화 컴포넌트.
 */

@Component
public class ScoreNormalizer {

    private static final double MAX_SCORE = 100.0;
    private static final double DEFAULT_SCORE = 50.0;

    //값이 낮을수록 좋은 지표 정규화
    public double normalizeInverse(double value, double min, double max) {
        if (max - min < 1e-9) {
            return DEFAULT_SCORE;
        }
        return (max - value) / (max - min) * MAX_SCORE;
    }

    //값이 높을수록 좋은 지표 정규화
    public double normalize(double value, double min, double max) {
        if (max - min < 1e-9) {
            return DEFAULT_SCORE;
        }
        return (value - min) / (max - min) * MAX_SCORE;
    }

    //컬렉션에서 최솟값 추출
    public double min(Collection<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    // 컬렉션에서 최댓값 추출
    public double max(Collection<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

}
