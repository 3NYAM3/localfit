package com.localfit.domain.region.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 법정동코드 데이터 수집 관련 설정
 * 수집 대상 법위를 설정으로 관리
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.region-code")
public class RegionSyncProperties {
    private List<String> targetKeywords;    // API 검색 키워드(수집 대상 범위)
    private List<String> targetSidoCodes;   // 수집 대상 시도코드
}
