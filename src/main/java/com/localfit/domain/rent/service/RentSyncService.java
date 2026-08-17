package com.localfit.domain.rent.service;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.client.RentTradeApiClient;
import com.localfit.domain.rent.config.RentSyncProperties;
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
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Double.parseDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentSyncService {

    private static final String SUCCESS_CODE = "000";   // API 정상 응답 코드
    private static final int MAX_RETRY = 3;                // API 호출 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;       // 재시도 간 대기시간 (시도마다 배로 증가)
    private static final long REQUEST_INTERVAL_MS = 300;   // 정상 페이지 요청 간 최소 간격 (Rate Limit 예방)
    private static final int NUM_OF_ROWS = 1000;           // 한 번에 가져올 갯수
    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final RentTradeApiClient rentTradeApiClient;
    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;
    private final RentSyncProperties properties;

    //수집 대상 시군구 전체x 최근 n개월 에 대해 전월세 실거래가를 동기화 시작
    @Transactional
    public int syncRecentMonths() {
        List<String> sigunguCodes = regionRepository.findDistinctSigunguCodes();
        List<String> dealYmds = recentYearMonths(properties.getMonthsToCollect());

        log.info("[RentSync] 동기화 시작 - 시군구 {}개 x {}개월 = 총 {}회 호출 예정",
                sigunguCodes.size(), dealYmds.size(), sigunguCodes.size() * dealYmds.size());

        int totalSaved = 0;
        for (String lawdCd : sigunguCodes) {
            for (String dealYmd : dealYmds) {
                totalSaved += syncOne(lawdCd, dealYmd);
                sleep(REQUEST_INTERVAL_MS);
            }
        }

        log.info("[RentSync] 동기화 완료 - 총 {}건 저장", totalSaved);
        return totalSaved;
    }

    //각 조합 1건 데이터 가져오기
    private int syncOne(String lawdCd, String dealYmd) {
        RentTradeApiResponse response = fetchWithRetry(lawdCd, dealYmd);

        if (!SUCCESS_CODE.equals(response.getResultCode())) {
            log.warn("[RentSync] lawdCd={}, dealYmd={} 응답코드 이상 - code: {}, msg: {}",
                    lawdCd, dealYmd, response.getResultCode(), response.getResultMsg());
            return 0;
        }

        int saved = 0;
        for (RentTradeApiResponse.Item item : response.getItems()) {
            if (saveIfNotExists(item, lawdCd)) {
                saved++;
            }
        }

        if (saved > 0) {
            log.info("[RentSync] lawdCd={}, dealYmd={} - {}건 저장", lawdCd, dealYmd, saved);
        }
        return saved;
    }

    // 실패시 재시도
    private RentTradeApiResponse fetchWithRetry(String lawdCd, String dealYmd) {
        CustomException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return rentTradeApiClient.fetch(lawdCd, dealYmd, 1, NUM_OF_ROWS);
            } catch (CustomException e) {
                lastException = e;
                log.warn("[RentSync] lawdCd={}, dealYmd={} 호출 실패 ({}번째 시도) - {}",
                        lawdCd, dealYmd, attempt, e.getMessage());
                sleep(RETRY_DELAY_MS * attempt);
            }
        }

        log.error("[RentSync] lawdCd={}, dealYmd={} 최대 재시도({}) 초과", lawdCd, dealYmd, MAX_RETRY);
        throw lastException;
    }

    //저장
    private boolean saveIfNotExists(RentTradeApiResponse.Item item, String lawdCd) {
        Region region = regionRepository
                .findByRegionCodeStartingWithAndDong(lawdCd, safeTrim(item.getUmdNm()))
                .orElse(null);

        if (region == null) {
            return false;  // 매칭되는 지역이 없으면 저장하지 않음
        }

        LocalDate dealDate = toDealDate(item);
        Double area = parseDouble(item.getExcluUseAr());
        Long deposit = parseAmount(item.getDeposit());

        if (dealDate == null || area == null) {
            return false;  // 필수 값이 파싱 불가하면 건너뜀
        }

        // 중복 체크 - 같은 기간을 여러 번 동기화해도 데이터가 중복 쌓이지 않도록
        boolean exists = rentTransactionRepository
                .existsByRegionAndAptNameAndExcluUseAreaAndDealDateAndDeposit(
                        region, safeTrim(item.getAptNm()), area, dealDate, deposit);
        if (exists) {
            return false;
        }

        RentTransaction transaction = RentTransaction.builder()
                .region(region)
                .aptName(safeTrim(item.getAptNm()))
                .excluUseArea(area)
                .dealDate(dealDate)
                .deposit(deposit)
                .monthlyRent(parseAmount(item.getMonthlyRent()))
                .floor(parseInteger(item.getFloor()))
                .buildYear(parseInteger(item.getBuildYear()))
                .build();

        rentTransactionRepository.save(transaction);
        return true;
    }

    //거래일자 localDate로 조합
    private LocalDate toDealDate(RentTradeApiResponse.Item item) {
        Integer year = parseInteger(item.getDealYear());
        Integer month = parseInteger(item.getDealMonth());
        Integer day = parseInteger(item.getDealDay());

        if (year == null || month == null || day == null) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    // 보증금 월세 ,로 연결된 문자열에서 분리된 숫자로 변환
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

    // 현재 월부터 과거로 N개월치 "yyyyMM" 문자열 목록 생성
    private List<String> recentYearMonths(int months) {
        YearMonth now = YearMonth.now();
        return IntStream.range(0, months)
                .mapToObj(i -> now.minusMonths(i).format(YM_FORMAT))
                .toList();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
