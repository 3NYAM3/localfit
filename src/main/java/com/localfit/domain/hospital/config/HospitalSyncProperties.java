package com.localfit.domain.hospital.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 병원 데이터 수집 관련 설정.
 * HIRA는 법정동 코드와 다른 자체 시도코드 체계를 사용하므롷 대상코드를 설정으로 분리하여 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-data.hospital")
public class HospitalSyncProperties {
    private List<String> targetSidoCodes;   // HIRA 시도 코드 (서울/인천/경기)
    private List<String> targetClCodes;     // 종별 코드 필터(상급병원/종합병원/병원)
}
