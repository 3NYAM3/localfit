package com.localfit.domain.rent.service;


import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.client.RentTradeApiClient;
import com.localfit.domain.rent.dto.RentTradeApiResponse;
import com.localfit.domain.rent.entity.RentTransaction;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.global.exception.CustomException;
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
 * 시군구 단위 전월세 데이터 동기화를 담당하는 서비스.
 * [클래스를 분리한 이유]
 * 같은 클래스 내부에서 메서드를 호출하면 Spring AOP 프록시를 거치지 않아
 * @Transactional이 적용되지 않는다(self-invocation 문제).
 * 시군구 단위로 트랜잭션을 나누려면 별도 빈으로 분리해야 한다.
 * [성능 최적화]
 * - Region을 시군구당 1회만 조회해 Map으로 캐싱 (거래 1건마다 조회하던 것을 제거)
 * - 중복 판별 키를 사전에 일괄 조회해 Set으로 비교 (건별 exists 쿼리 제거)
 * - 수집 완료 후 saveAll()로 일괄 저장
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RentSigunguSyncService {
    private static final String SUCCESS_CODE = "000";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long REQUEST_INTERVAL_MS = 200;
    private static final int NUM_OF_ROWS = 1000;
    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final RentTradeApiClient rentTradeApiClient;
    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;

     // 시군구 1개에 대해 전체 대상 기간을 동기화한다.
     // 이 메서드 단위로 트랜잭션이 걸려, 중간에 실패해도 앞서 처리한 시군구는 보존된다.
    @Transactional
    public int syncSigungu(String lawdCd, List<String> dealYmds) {
        // 1) 이 시군구의 Region을 한 번에 조회해 "동 이름 → Region" Map으로 캐싱
        Map<String, Region> regionMap = regionRepository.findByRegionCodeStartingWith(lawdCd)
                .stream()
                .filter(r -> r.getDong() != null && !r.getDong().isBlank())
                .collect(Collectors.toMap(Region::getDong, Function.identity(), (a, b) -> a));

        if (regionMap.isEmpty()) {
            log.warn("[RentSync] lawdCd={} - 매칭되는 Region 없음, 건너뜀", lawdCd);
            return 0;
        }

        // 2) 이 시군구의 기존 데이터 키를 한 번에 조회해 Set으로 캐싱
        LocalDate rangeStart = YearMonth.parse(dealYmds.getLast(), YM_FORMAT).atDay(1);
        LocalDate rangeEnd = YearMonth.parse(dealYmds.getFirst(), YM_FORMAT).atEndOfMonth();
        Set<String> existingKeys = new HashSet<>(
                rentTransactionRepository.findDuplicateKeys(lawdCd, rangeStart, rangeEnd));

        // 3) 월별로 수집 (페이지네이션 포함)
        List<RentTransaction> toSave = new ArrayList<>();
        for (String dealYmd : dealYmds) {
            toSave.addAll(collectMonth(lawdCd, dealYmd, regionMap, existingKeys));
            sleep(REQUEST_INTERVAL_MS);
        }

        // 4) 일괄 저장
        if (!toSave.isEmpty()) {
            rentTransactionRepository.saveAll(toSave);
        }

        log.info("[RentSync] lawdCd={} 완료 - {}건 저장", lawdCd, toSave.size());
        return toSave.size();
    }

    // 특정 시군구/월의 데이터를 페이지네이션으로 끝까지 수집해 Entity 목록으로 변환한다.
    // DB 저장은 하지 않고 목록만 반환한다(호출부에서 일괄 저장).
    private List<RentTransaction> collectMonth(String lawdCd, String dealYmd,
                                               Map<String, Region> regionMap,
                                               Set<String> existingKeys) {
        List<RentTransaction> result = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            RentTradeApiResponse response = fetchWithRetry(lawdCd, dealYmd, pageNo);

            if (!SUCCESS_CODE.equals(response.getResultCode())) {
                log.warn("[RentSync] lawdCd={}, dealYmd={} 응답코드 이상 - {}",
                        lawdCd, dealYmd, response.getResultCode());
                break;
            }

            List<RentTradeApiResponse.Item> items = response.getItems();
            if (items.isEmpty()) {
                break;
            }

            for (RentTradeApiResponse.Item item : items) {
                RentTransaction transaction = toEntity(item, regionMap, existingKeys);
                if (transaction != null) {
                    result.add(transaction);
                }
            }

            // 가져온 건수가 요청 건수보다 적으면 마지막 페이지
            if (items.size() < NUM_OF_ROWS) {
                break;
            }
            pageNo++;
            sleep(REQUEST_INTERVAL_MS);
        }

        return result;
    }


    // API 응답 1건을 Entity로 변환한다.
    // Region 매칭 실패, 필수값 파싱 실패, 중복인 경우 null을 반환한다.
    private RentTransaction toEntity(RentTradeApiResponse.Item item,
                                     Map<String, Region> regionMap,
                                     Set<String> existingKeys) {
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

        // 중복 체크 - DB 조회 없이 메모리에서 판별
        // add()가 false면 이미 존재 (기존 DB 데이터 + 같은 배치 내 중복 모두 걸러짐)
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

    //중복 판별용 키 생성 - Repository의 CONCAT 쿼리와 형식이 일치해야 한다
    private String buildKey(Long regionId, String aptName, Double area, LocalDate dealDate, Long deposit) {
        return regionId + "|" + aptName + "|" + area + "|" + dealDate + "|" + deposit;
    }

    // 외부 API 호출 - 실패 시 지수 백오프로 재시도
    private RentTradeApiResponse fetchWithRetry(String lawdCd, String dealYmd, int pageNo) {
        CustomException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return rentTradeApiClient.fetch(lawdCd, dealYmd, pageNo, NUM_OF_ROWS);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[RentSync] lawdCd={}, dealYmd={}, page={} 호출 실패 ({}회)",
                        lawdCd, dealYmd, pageNo, attempt);
                sleep(RETRY_DELAY_MS * attempt);
            }
        }
        throw lastException;
    }

    private LocalDate toDealDate(RentTradeApiResponse.Item item) {
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

    // "29,768" 같은 콤마 포함 문자열을 숫자로 변환 (전세는 월세가 "0")
    private Long parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
