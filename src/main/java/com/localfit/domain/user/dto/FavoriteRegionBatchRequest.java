package com.localfit.domain.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관심지역 일관 등록 요청
 */
@Getter
@NoArgsConstructor
public class FavoriteRegionBatchRequest {

    @NotEmpty(message = "관심지역은 최소 1개 이상 선택해야 합니다.")
    @Size(max = 5, message = "관심지역은 최대 5개까지 등록할 수 있습니다.")
    @Valid
    private List<FavoriteRegionRequest> favorites;
}
