package com.localfit.domain.hospital.controller;

import com.localfit.domain.hospital.service.HospitalSyncService;
import com.localfit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HospitalSyncController {

    private final HospitalSyncService hospitalSyncService;

    @PostMapping("/internal/hospital/sync")
    public ApiResponse<String> sync(){
        int count = hospitalSyncService.sync();
        return ApiResponse.ok("병원 동기화 완료,", count +"건 저장");
    }
}
