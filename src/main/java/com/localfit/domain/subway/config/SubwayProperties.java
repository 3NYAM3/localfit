package com.localfit.domain.subway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 지하철역 파일데이터 임포트 관련 설정
 * 파일경로를 설정으로 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.subway")
public class SubwayProperties {
    private String dataFilePath;
}
