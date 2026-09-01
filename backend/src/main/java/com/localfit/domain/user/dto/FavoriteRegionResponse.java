package com.localfit.domain.user.dto;

import com.localfit.domain.user.entity.FavoriteRegion;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관심지역 조회 응답
 */
@Getter
public class FavoriteRegionResponse {

    private final Long favoriteId;
    private final Long regionId;
    private final Integer priority;
    private final String sido;
    private final String sigungu;
    private final String dong;
    private final LocalDateTime addedAt;

    public FavoriteRegionResponse(FavoriteRegion favoriteRegion) {
        this.favoriteId = favoriteRegion.getId();
        this.regionId = favoriteRegion.getRegion().getId();
        this.priority = favoriteRegion.getPriority();
        this.sido = favoriteRegion.getRegion().getSido();
        this.sigungu = favoriteRegion.getRegion().getSigungu();
        this.dong = favoriteRegion.getRegion().getDong();
        this.addedAt = favoriteRegion.getCreatedAt();
    }
}
