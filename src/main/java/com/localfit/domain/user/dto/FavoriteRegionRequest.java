package com.localfit.domain.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteRegionRequest {

    @NotNull
    private Long regionId;

    @NotNull
    @Min(1)
    private Integer priority;
}
