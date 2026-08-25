package com.localfit.domain.recommendation.dto;

import com.localfit.domain.user.entity.ImportanceLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 관심지역 점수 조회시 지표별 중요도를 담는 요청.
 */
@Getter
@NoArgsConstructor
public class ScoreWeightRequest {

    private Map<IndicatorType, ImportanceLevel> importance = defaultImportance();

    /** 요청에 값이 없으면 모든 지표를 NORMAL(보통)로 취급 */
    private static Map<IndicatorType, ImportanceLevel> defaultImportance() {
        Map<IndicatorType, ImportanceLevel> map = new EnumMap<>(IndicatorType.class);
        for (IndicatorType type : IndicatorType.values()) {
            map.put(type, ImportanceLevel.NORMAL);
        }
        return map;
    }

    public ImportanceLevel get(IndicatorType type) {
        return importance.getOrDefault(type, ImportanceLevel.NORMAL);
    }
}
