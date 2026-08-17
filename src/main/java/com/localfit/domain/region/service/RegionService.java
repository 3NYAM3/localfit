package com.localfit.domain.region.service;

import com.localfit.domain.region.dto.RegionResponse;
import com.localfit.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Region 조회를 담당하는 서비스.
 * 동기화(RegionSyncService)와 책임을 분리했다
 */

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;

    @Transactional(readOnly = true) // 조회 전용 - 변경 감지(dirty checking) 비용을 줄여줌
    public Page<RegionResponse> searchRegions(String sido, String sigungu, String keyword, Pageable pageable){
        return  regionRepository.search(sido, sigungu, keyword, pageable).map(RegionResponse::new);
    }
}
