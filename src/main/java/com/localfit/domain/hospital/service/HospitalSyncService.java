package com.localfit.domain.hospital.service;

import com.localfit.domain.hospital.client.HiraHospitalApiClient;
import com.localfit.domain.hospital.config.HospitalSyncProperties;
import com.localfit.domain.hospital.dto.HospitalApiResponse;
import com.localfit.domain.hospital.entity.Hospital;
import com.localfit.domain.hospital.repository.HospitalRepository;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HIRA 병원정보서비스에서 데이터를 가져와 Hospital 테이블에 통기화하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalSyncService {

    private static final String SUCCESS_CODE = "00";        // API 정상 응답 코드
    private static final int MAX_RETRY = 3;                 // API 호출 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;        // 재시도 간 대기시간 (시도마다 배로 증가)
    private static final long REQUEST_INTERVAL_MS = 300;    // 정상 페이지 요청 간 최소 간격 (Rate Limit 예방)
    private static final int NUM_OF_ROWS = 500;             // 페이지당 결과 수

    private final HiraHospitalApiClient apiClient;
    private final HospitalRepository hospitalRepository;
    private final RegionRepository regionRepository;
    private final HiraRegionNormalizer regionNormalizer;
    private final HospitalSyncProperties properties;

    /**
     * 설정된 시도/종별코드 조합으로 수도권 병원 데이터를 수집해 저장
     *
     * @return 저장된 병원 건수
     */
    @Transactional
    public int sync() {
        // 시군구 텍스트 매칭을 위해 전체 Region을 1회 조회해 인덱스로 캐싱
        Map<String, Region> regionIndex = regionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        r -> r.getSido() + "|" + r.getSigungu(),
                        Function.identity(),
                        (a, b) -> a
                ));

        int totalSaved = 0;
        int totalSkipped = 0;

        for (String sidoCd : properties.getTargetSidoCodes()) {
            for (String clCd : properties.getTargetClCodes()) {
                SyncResult result = syncBySidoAndClCd(sidoCd, clCd, regionIndex);
                totalSaved += result.saved();
                totalSkipped += result.skipped();
            }
        }

        log.info("[HospitalSync] 전체 완료 - 저장 {}건, 매칭 실패로 제외 {}건", totalSaved, totalSkipped);
        return totalSaved;
    }

    /** 시도/종별코드 1개 조합에 대해 페이지네이션으로 끝까지 수집해 저장 */
    private SyncResult syncBySidoAndClCd(String sidoCd, String clCd, Map<String, Region> regionIndex) {
        int pageNo = 1;
        int saved = 0;
        int skipped = 0;

        while (true) {
            HospitalApiResponse response = fetchWithRetry(sidoCd, clCd, pageNo);

            validateResponse(response, sidoCd, clCd);

            List<HospitalApiResponse.HospitalItem> items = response.getItems();
            if (items.isEmpty()) {
                break;
            }

            for (HospitalApiResponse.HospitalItem item : items) {
                if (saveIfMatched(item, regionIndex)) {
                    saved++;
                } else {
                    skipped++;
                }
            }

            if (items.size() < NUM_OF_ROWS) {
                break;
            }
            pageNo++;
            sleep(REQUEST_INTERVAL_MS);
        }

        log.info("[HospitalSync] sidoCd={}, clCd={} 완료 - 저장 {}건, 제외 {}건", sidoCd, clCd, saved, skipped);
        return new SyncResult(saved, skipped);
    }

    /**
     * 병원 1건을 Region과 매칭해 저장
     * ykiho 중복이거나 Region 매칭 실패 시 false를 반환
     */
    private boolean saveIfMatched(HospitalApiResponse.HospitalItem item, Map<String, Region> regionIndex) {
        if (hospitalRepository.existsByYkiho(item.getYkiho())) {
            return false;
        }

        String normalizedSido = regionNormalizer.normalizeSido(item.getSidoName());
        String normalizedSigungu = regionNormalizer.normalizeSigungu(item.getSidoName(), item.getSigunguName());

        Region region = regionIndex.get(normalizedSido + "|" + normalizedSigungu);
        if (region == null) {
            log.debug("[HospitalSync] Region 매칭 실패 - {} {}", normalizedSido, normalizedSigungu);
            return false;
        }

        hospitalRepository.save(Hospital.builder()
                .ykiho(item.getYkiho())
                .hospitalName(item.getHospitalName())
                .clCode(item.getClCode())
                .clCodeName(item.getClCodeName())
                .region(region)
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .build());

        return true;
    }

    /** API 응답 코드를 검증한다. 정상(00)이 아니면 예외를 던진다 */
    private void validateResponse(HospitalApiResponse response, String sidoCd, String clCd) {
        if (!SUCCESS_CODE.equals(response.getResultCode())) {
            log.error("[HospitalSync] sidoCd={}, clCd={} 응답코드 이상 - code: {}, msg: {}", sidoCd, clCd, response.getResultCode(), response.getResultMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** API 호출 실패 시 지수 백오프로 최대 MAX_RETRY회 재시도 */
    private HospitalApiResponse fetchWithRetry(String sidoCd, String clCd, int pageNo) {
        CustomException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return apiClient.fetch(sidoCd, clCd, pageNo, NUM_OF_ROWS);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[HospitalSync] sidoCd={}, clCd={}, page={} 호출 실패 ({}회)", sidoCd, clCd, pageNo, attempt);
                sleep(RETRY_DELAY_MS * attempt);
            }
        }
        throw lastException;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 시도 1개 처리 결과 (저장/제외 건수) */
    private record SyncResult(int saved, int skipped) {
    }
}
