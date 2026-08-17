package com.localfit.domain.region.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * application.properties의 "public-data.region-code.*" 설정값을 객체로 바인딩.
 *
 * [설계 이유] 동기화 대상 지역 범위(수도권 등)를 자바 코드에 하드코딩하지 않고
 * 설정 파일로 분리했다. 나중에 지역 범위를 넓히거나 좁힐 때 코드 수정 없이
 * application.properties의 값만 바꾸면 되도록 하기 위함.
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.region-code")
public class RegionSyncProperties {
    private List<String> targetKeywords;
    private List<String> targetSidoCodes;
}
