package com.localfit.domain.region.dto;


import com.localfit.domain.region.entity.Region;
import lombok.Getter;

/**
 * 지역 조회 API의 응답 형태.
 */

@Getter
public class RegionResponse {
    private final Long id;
    private final String regionCode;
    private final String sido;
    private final String sigungu;
    private final String dong;

    public RegionResponse(Region region){
        this.id  = region.getId();
        this.regionCode = region.getRegionCode();
        this.sido = region.getSido();
        this.sigungu = region.getSigungu();
        this.dong = region.getDong();
    }
}
