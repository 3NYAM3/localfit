package com.localfit.domain.rent.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 전월세 실거래가 데이터 수집 관련 설정
 * 수집 개월 수를 설정으로 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.rent-trade")
public class RentSyncProperties {
    private int monthsToCollect;    //수집할 최근 개월 수
}
