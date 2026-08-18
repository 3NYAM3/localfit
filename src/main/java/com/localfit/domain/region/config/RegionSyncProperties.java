package com.localfit.domain.region.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * application.properties의 "public-data.region-code.*" 설정값을 객체로 바인딩.
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.region-code")
public class RegionSyncProperties {
    private List<String> targetKeywords;
    private List<String> targetSidoCodes;
}
