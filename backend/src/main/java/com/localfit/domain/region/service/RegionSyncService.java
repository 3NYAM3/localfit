package com.localfit.domain.region.service;

import com.localfit.domain.region.client.RegionCodeApiClient;
import com.localfit.domain.region.config.RegionSyncProperties;
import com.localfit.domain.region.dto.RegionApiResponse;
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
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionSyncService {

    private static final String SUCCESS_CODE = "INFO-0";    // API 정상 응답 코드
    private static final int MAX_RETRY = 3;                 // API 호출 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;        // 재시도 간 대기시간 (시도마다 배로 증가)
    private static final long REQUEST_INTERVAL_MS = 300;    // 정상 페이지 요청 간 최소 간격 (Rate Limit 예방)
    private static final int NUM_OF_ROWS = 100;             // 페이지당 결과 수

    private final RegionCodeApiClient regionCodeApiClient;
    private final RegionRepository regionRepository;
    private final RegionSyncProperties properties;

    /**
     * 설정된 키워드/시도코드 기준으로 수도권 법정동 데이터를 수집해 저장
     *
     * @return 저장된 법정동 건수
     */
    @Transactional
    public int sync() {
        Set<String> targetSidoCodes = Set.copyOf(properties.getTargetSidoCodes());

        int totalSaved = 0;
        for (String keyword : properties.getTargetKeywords()) {
            totalSaved += syncByKeyword(keyword, targetSidoCodes);
        }
        return totalSaved;
    }

    /** 키워드1개에 대해 페이지네이션으로 끝까지 수집해 저장 */
    private int syncByKeyword(String keyword, Set<String> targetSidoCodes) {
        int pageNo = 1;
        int savedCount = 0;

        while (true) {
            RegionApiResponse response = fetchWithRetry(keyword, pageNo, NUM_OF_ROWS);

            validateResponse(response, keyword);

            List<RegionApiResponse.Item> items = response.getItems();
            if (items.isEmpty()) {
                break;
            }

            //대상 시도에 해당하는 항목만 필터링해서 저장
            for (RegionApiResponse.Item item : items) {
                if (targetSidoCodes.contains(item.getSidoCd()) && saveIfNotExists(item)) {
                    savedCount++;
                }
            }

            if (items.size() < NUM_OF_ROWS) {
                break;
            }
            pageNo++;
            sleep(REQUEST_INTERVAL_MS);
        }

        log.info("[RegionSync] '{}' 동기화 완료 - 신규 저장 {}건", keyword, savedCount);
        return savedCount;
    }

    /**
     * 법정동 1건을 저장
     * 이미 존재하거나 시/구 레벨 코드(뒤 5자리 00000)면 false를 반환
     */
    private boolean saveIfNotExists(RegionApiResponse.Item item) {
        String regionCode = item.getRegionCd();

        // 뒤 5자리가 전부 0이면 실제 동이 아니라 시/구 레벨 상위 코드이므로 제외
        if (regionCode.endsWith("00000")) {
            return false;
        }

        if (regionRepository.existsByRegionCode(regionCode)) {
            return false;
        }

        regionRepository.save(Region.builder()
                .regionCode(regionCode)
                .sido(extractSido(item))
                .sigungu(extractSigungu(item))
                .dong(item.getLocallowNm())
                .build());

        return true;
    }

    /** API 응답 코드를 검증한다. 정상(INFO-0)이 아니면 예외를 던진다 */
    private void validateResponse(RegionApiResponse response, String keyword) {
        String resultCode = response.getResultCode();

        if (resultCode == null) {
            log.error("[RegionSync] '{}' 응답 형식 오류 - resultCode 없음", keyword);
            throw new CustomException(ErrorCode.EXTERNAL_API_INVALID_RESPONSE);
        }

        if (!SUCCESS_CODE.equals(resultCode)) {
            log.error("[RegionSync] '{}' API 오류 - code: {}, msg: {}", keyword, resultCode, response.getResultMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** API 호출 실패 시 지수 백오프로 최대 MAX_RETRY회 재시도 */
    private RegionApiResponse fetchWithRetry(String keyword, int pageNo, int numOfRows) {
        CustomException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return regionCodeApiClient.fetch(keyword, pageNo, numOfRows);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[RegionSync] '{}' 페이지 {} 호출 실패 ({}번째 시도) - {}", keyword, pageNo, attempt, e.getMessage());
                sleep(RETRY_DELAY_MS * attempt); // 재시도마다 대기시간 증가 (지수 백오프)
            }
        }

        log.error("[RegionSync] '{}' 페이지 {} 최대 재시도({}) 초과", keyword, pageNo, MAX_RETRY);
        throw lastException;
    }

    /** locatadd_nm(전체 주소명)에서 시도명을 추출한다. 예: "서울특별시 송파구 잠실동" → "서울특별시" */
    private String extractSido(RegionApiResponse.Item item) {
        String[] parts = item.getLocatAddNm().split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    /** locatadd_nm(전체 주소명)에서 시군구명을 추출한다. 예: "서울특별시 송파구 잠실동" → "송파구" */
    private String extractSigungu(RegionApiResponse.Item item) {
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
