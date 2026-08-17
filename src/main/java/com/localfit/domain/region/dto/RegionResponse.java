package com.localfit.domain.region.dto;


import com.localfit.domain.region.entity.Region;
import lombok.Getter;

/**
 * 지역 조회 API의 응답 형태.
 * Entity를 그대로 반환하지 않고 DTO로 변환해서 내려주는 이유:
 * - Entity의 내부 구조(연관관계, JPA 어노테이션 등)가 API 스펙에 그대로 노출되는 걸 방지
 * - 나중에 Entity가 바뀌어도 API 응답 스펙은 별도로 관리 가능
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
