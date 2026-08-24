package com.localfit.domain.subway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.subway")
public class SubwayProperties {
    private String dataFilePath;
}
