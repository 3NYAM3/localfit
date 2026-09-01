package com.localfit.domain.rent.service;


import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.client.RentTradeApiClient;
import com.localfit.domain.rent.dto.RentApiResponse;
import com.localfit.domain.rent.entity.RentTransaction;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.global.exception.CustomException;
import com.localfit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시군구 1개 단위로 전월세 실거래가를 수집해 저장하는 서비스
 * <p>
 * 수집 저장은 이 서비스에서 담당한다.
 *
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RentSigunguSyncService {

    private static final String SUCCESS_CODE = "000";          // API 정상 응답 코드
    private static final int MAX_RETRY = 3;                    // API 호출 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;           // 재시도 간 대기시간 (시도마다 배로 증가)
    private static final long REQUEST_INTERVAL_MS = 200;       // 페이지 요청 간 최소 간격 (Rate Limit 예방)
    private static final int NUM_OF_ROWS = 1000;               // 페이지당 결과 수
    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final RentTradeApiClient rentTradeApiClient;
    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;

    /**
     * 시군구 1개에 대해 전체 대상 기간의 전월세 실거래가를 수집해 저장
     *
     * @param lawdCd   시군구 법정동코드 앞 5자리
     * @param dealYmds 수집 대상 년월 목록(yyyymm, 최신순)
     * @return 저장된 거래 건수
     */
    @Transactional
    public int syncSigungu(String lawdCd, List<String> dealYmds) {
        // 이 시군구의 Region을 한 번에 조회해 "동 이름 → Region" Map으로 캐싱
        Map<String, Region> regionMap = regionRepository.findByRegionCodeStartingWith(lawdCd)
                .stream()
                .filter(r -> r.getDong() != null && !r.getDong().isBlank())
                .collect(Collectors.toMap(Region::getDong, Function.identity(), (a, b) -> a));

        if (regionMap.isEmpty()) {
            log.warn("[RentSync] lawdCd={} - 매칭되는 Region 없음, 건너뜀", lawdCd);
            return 0;
        }

        // 이 시군구의 기존 데이터 키를 한 번에 조회해 Set으로 캐싱
        LocalDate rangeStart = YearMonth.parse(dealYmds.getLast(), YM_FORMAT).atDay(1);
        LocalDate rangeEnd = YearMonth.parse(dealYmds.getFirst(), YM_FORMAT).atEndOfMonth();
        Set<String> existingKeys = new HashSet<>(
                rentTransactionRepository.findDuplicateKeys(lawdCd, rangeStart, rangeEnd));

        // 월별로 수집 후 일괄 저장
        List<RentTransaction> toSave = new ArrayList<>();
        for (String dealYmd : dealYmds) {
            toSave.addAll(collectMonth(lawdCd, dealYmd, regionMap, existingKeys));
            sleep(REQUEST_INTERVAL_MS);
        }

        if (!toSave.isEmpty()) {
            rentTransactionRepository.saveAll(toSave);
        }

        log.info("[RentSync] lawdCd={} 완료 - {}건 저장", lawdCd, toSave.size());
        return toSave.size();
    }

    /**
     * 특정 시군구/월의 데이터를 페이지네이션으로 끝까지 수집해 Entity 목록으로 반환한다.
     * DB 저장은 하지 않고 목록만 반환한다 (호출부에서 일괄 저장).
     */
    private List<RentTransaction> collectMonth(String lawdCd, String dealYmd, Map<String, Region> regionMap, Set<String> existingKeys) {
        List<RentTransaction> result = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            RentApiResponse response = fetchWithRetry(lawdCd, dealYmd, pageNo);

            validateResponse(response, lawdCd, dealYmd);

            List<RentApiResponse.Item> items = response.getItems();
            if (items.isEmpty()) {
                break;
            }

            for (RentApiResponse.Item item : items) {
                RentTransaction transaction = toEntity(item, regionMap, existingKeys);
                if (transaction != null) {
                    result.add(transaction);
                }
            }

            if (items.size() < NUM_OF_ROWS) {
                break;
            }
            pageNo++;
            sleep(REQUEST_INTERVAL_MS);
        }

        return result;
    }

    /**
     * API 응답 1건을 RentTransaction 엔티티로 변환한다.
     * Region 매칭 실패, 필수값 파싱 실패, 중복인 경우 null을 반환한다.
     */
    private RentTransaction toEntity(RentApiResponse.Item item, Map<String, Region> regionMap, Set<String> existingKeys) {
        Region region = regionMap.get(safeTrim(item.getUmdNm()));
        if (region == null) {
            return null;
        }

        LocalDate dealDate = toDealDate(item);
        Double area = parseDouble(item.getExcluUseAr());
        if (dealDate == null || area == null) {
            return null;
        }

        Long deposit = parseAmount(item.getDeposit());
        String aptName = safeTrim(item.getAptNm());

        // add()가 false면 이미 존재 — 기존 DB 데이터와 같은 배치 내 중복 모두 걸러짐
        String key = buildKey(region.getId(), aptName, area, dealDate, deposit);
        if (!existingKeys.add(key)) {
            return null;
        }

        return RentTransaction.builder()
                .region(region)
                .aptName(aptName)
                .excluUseArea(area)
                .dealDate(dealDate)
                .deposit(deposit)
                .monthlyRent(parseAmount(item.getMonthlyRent()))
                .floor(parseInteger(item.getFloor()))
                .buildYear(parseInteger(item.getBuildYear()))
                .build();
    }

    /** API 응답 코드를 검증한다. 정상(000)이 아니면 예외를 던진다 */
    private void validateResponse(RentApiResponse response, String lawdCd, String dealYmd) {
        if (!SUCCESS_CODE.equals(response.getResultCode())) {
            log.error("[RentSync] lawdCd={}, dealYmd={} 응답코드 이상 - {}", lawdCd, dealYmd, response.getResultCode());
            throw new CustomException(response.getResultCode().equals("000")
                    ? ErrorCode.EXTERNAL_API_INVALID_RESPONSE
                    : ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** API 응답의 년/월/일을 LocalDate로 변환한다. 파싱 실패 시 null을 반환한다 */
    private RentApiResponse fetchWithRetry(String lawdCd, String dealYmd, int pageNo) {
        CustomException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return rentTradeApiClient.fetch(lawdCd, dealYmd, pageNo, NUM_OF_ROWS);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[RentSync] lawdCd={}, dealYmd={}, page={} 호출 실패 ({}회)", lawdCd, dealYmd, pageNo, attempt);
                sleep(RETRY_DELAY_MS * attempt);
            }
        }

        log.error("[RentSync] lawdCd={}, dealYmd={}, page={} 최대 재시도({}) 초과", lawdCd, dealYmd, pageNo, MAX_RETRY);
        throw lastException;
    }

    /** 중복 판별용 키를 생성한다. Repository의 CONCAT 쿼리와 형식이 일치해야 한다 */
    private String buildKey(Long regionId, String aptName, Double area, LocalDate dealDate, Long deposit) {
        return regionId + "|" + aptName + "|" + area + "|" + dealDate + "|" + deposit;
    }

    /** API 응답의 년/월/일을 LocalDate로 변환한다. 파싱 실패 시 null을 반환한다 */
    private LocalDate toDealDate(RentApiResponse.Item item) {
        Integer year = parseInteger(item.getDealYear());
        Integer month = parseInteger(item.getDealMonth());
        Integer day = parseInteger(item.getDealDay());

        if (year == null || month == null || day == null) return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /** "29,768" 같은 콤마 포함 문자열을 Long으로 변환한다. 파싱 실패 시 0을 반환한다 */
    private Long parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 문자열을 Double로 변환한다. 파싱 실패 시 null을 반환한다 */
    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 문자열을 Integer로 변환한다. 파싱 실패 시 null을 반환한다 */
    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeTrim(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
