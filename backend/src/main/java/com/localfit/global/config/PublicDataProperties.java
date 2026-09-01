package com.localfit.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 공공데이터포털 공통설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data")
public class PublicDataProperties {
    private String serviceKey;
}
