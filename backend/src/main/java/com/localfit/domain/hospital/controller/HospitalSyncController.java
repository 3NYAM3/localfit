package com.localfit.domain.hospital.controller;

import com.localfit.domain.hospital.service.HospitalSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 병원 데이터 수집 트리거 API
 */
@RestController
@RequiredArgsConstructor
public class HospitalSyncController {

    private final HospitalSyncService hospitalSyncService;

    /**
     * HIRA 병원정보서비스에서 수도권 상급병원/종합병원/병원 데이터를 수집
     *
     * @return 저장된 병원 건수
     */
    @PostMapping("/internal/hospital/sync")
    public ApiResponse<String> sync(){
        int count = hospitalSyncService.sync();
        return ApiResponse.ok("병원 동기화 완료,", count +"건 저장");
    }
}
