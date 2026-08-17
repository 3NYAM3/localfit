package com.localfit.domain.rent.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.rent-trade")
public class RentSyncProperties {
    private int monthsToCollect;//수집할 최근 개월 수
}
