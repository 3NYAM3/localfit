package com.localfit.domain.hospital.dto;

/**
 * 시군구 단위 병원 개수 집계 결과
 */
public interface HospitalCountProjection {
    String getSido();

    String getSigungu();

    Long getHospitalCount();
}
