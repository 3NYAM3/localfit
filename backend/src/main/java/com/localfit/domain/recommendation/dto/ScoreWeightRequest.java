package com.localfit.domain.recommendation.dto;

import com.localfit.domain.user.entity.ImportanceLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;

/**
 * 관심지역 점수 조회시 지표별 중요도를 담는 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class ScoreWeightRequest {

    private ImportanceLevel housingImportance = ImportanceLevel.NORMAL;
    private ImportanceLevel subwayImportance = ImportanceLevel.NORMAL;
    private ImportanceLevel hospitalImportance = ImportanceLevel.NORMAL;

    /**
     * 특정 지표의 중요도를 반환한다.
     *
     * @param type 조회할 지표
     * @return 해당 지표의 중요도 등급
     */
    public ImportanceLevel get(IndicatorType type) {
        return switch (type) {
            case HOUSING -> housingImportance;
            case SUBWAY -> subwayImportance;
            case HOSPITAL -> hospitalImportance;
        };
    }
}
