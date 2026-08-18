package com.localfit.domain.region.service;

import com.localfit.domain.region.client.RegionCodeApiClient;
import com.localfit.domain.region.config.RegionSyncProperties;
import com.localfit.domain.region.dto.RegionCodeApiResponse;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static java.lang.Thread.sleep;

/**
 * 법정동코드 API에서 가져온 데이터를 Region 테이블에 동기화하는 서비스.
 * 동기화 대상 지역 범위는 RegionSyncProperties(설정)를 통해 주입받는다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionSyncService {

    private static final String SUCCESS_CODE = "INFO-0";   // API 정상 응답 코드
    private static final int MAX_RETRY = 3;                // API 호출 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;       // 재시도 간 대기시간 (시도마다 배로 증가)
    private static final long REQUEST_INTERVAL_MS = 300;   // 정상 페이지 요청 간 최소 간격 (Rate Limit 예방)
    private static final int NUM_OF_ROWS = 100;

    private final RegionCodeApiClient regionCodeApiClient;
    private final RegionRepository regionRepository;
    private final RegionSyncProperties properties;

    @Transactional
    public int syncTargetRegions() {
        Set<String> targetSidoCodes = Set.copyOf(properties.getTargetSidoCodes());

        int totalSaved = 0;
        for (String keyword : properties.getTargetKeywords()) {
            totalSaved += syncByKeyword(keyword, targetSidoCodes);
        }
        return totalSaved;
    }

    private int syncByKeyword(String keyword, Set<String> targetSidoCodes) {
        int pageNo = 1;
        int savedCount = 0;

        while (true) {
            RegionCodeApiResponse response = fetchWithRetry(keyword, pageNo, NUM_OF_ROWS);

            validateResponse(response, keyword);

            List<RegionCodeApiResponse.RegionCodeItem> items = response.getItems();
            if (items.isEmpty()) {
                break;
            }

            // 대상 시도(수도권 등)에 해당하는 항목만 필터링해서 저장
            for (RegionCodeApiResponse.RegionCodeItem item : items) {
                if (targetSidoCodes.contains(item.getSidoCd()) && saveIfNotExists(item)) {
                    savedCount++;
                }
            }

            //log.info("[RegionSync] '{}' 페이지 {} 처리 완료 ({}건)", keyword, pageNo, items.size());

            if (items.size() < NUM_OF_ROWS) {
                break; // 마지막 페이지 도달
            }
            pageNo++;
            sleep(REQUEST_INTERVAL_MS); // 다음 페이지 요청 전 짧게 대기 (연속 호출로 인한 제한 방지)
        }

        log.info("[RegionSync] '{}' 동기화 완료 - 신규 저장 {}건", keyword, savedCount);
        return savedCount;
    }

    // 실패시 재시도
    private RegionCodeApiResponse fetchWithRetry(String keyword, int pageNo, int numOfRows) {
        CustomException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return regionCodeApiClient.fetchByAddressKeyword(keyword, pageNo, numOfRows);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[RegionSync] '{}' 페이지 {} 호출 실패 ({}번째 시도) - {}",
                        keyword, pageNo, attempt, e.getMessage());
                sleep(RETRY_DELAY_MS * attempt); // 재시도마다 대기시간 증가 (지수 백오프)
            }
        }

        log.error("[RegionSync] '{}' 페이지 {} 최대 재시도({}) 초과", keyword, pageNo, MAX_RETRY);
        throw lastException;
    }

    private void validateResponse(RegionCodeApiResponse response, String keyword) {
        String resultCode = response.getResultCode();

        if (resultCode == null) {
            log.error("[RegionSync] '{}' 응답 형식 오류 - resultCode 없음", keyword);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        if (!SUCCESS_CODE.equals(resultCode)) {
            log.error("[RegionSync] '{}' API 오류 - code: {}, msg: {}",
                    keyword, resultCode, response.getResultMsg());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private boolean saveIfNotExists(RegionCodeApiResponse.RegionCodeItem item) {
        if (regionRepository.existsByRegionCode(item.getRegionCd())) {
            return false;
        }

        Region region = Region.builder()
                .regionCode(item.getRegionCd())
                .sido(extractSido(item))
                .sigungu(extractSigungu(item))
                .dong(item.getLocallowNm())
                .build();

        regionRepository.save(region);
        return true;
    }

    //locatadd_nm(전체 주소명)에서 시도명 추출. 예: "서울특별시 송파구 잠실동" → "서울특별시"
    private String extractSido(RegionCodeApiResponse.RegionCodeItem item) {
        String[] parts = item.getLocatAddNm().split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    //locatadd_nm(전체 주소명)에서 시군구명 추출. 예: "서울특별시 송파구 잠실동" → "송파구"
    private String extractSigungu(RegionCodeApiResponse.RegionCodeItem item) {
        String[] parts = item.getLocatAddNm().split(" ");
        return parts.length > 1 ? parts[1] : "";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
