package com.localfit.domain.rent.service;

import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.config.RentSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 전월세 실거래가 동기화 전체 흐름을 조율하는 서비스.
 * 실제 수집/저장은 RentSigunguSyncService가 시군구 단위 트랜잭션으로 처리한다.
 * 이 클래스에는 @Transactional을 걸지 않는다 - 전체를 하나의 트랜잭션으로 묶으면
 * DB 커넥션을 수십 분간 점유하고, 마지막에 실패 시 전부 롤백되기 때문.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RentSyncService {

    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final RegionRepository regionRepository;
    private final RentSigunguSyncService rentSigunguSyncService;
    private final RentSyncProperties properties;

    //수집 대상 시군구 전체 x 최근 N개월에 대해 전월세 실거래가를 동기화한다. (진입점)
    public int syncRecentMonths() {
        long startTime = System.currentTimeMillis();

        List<String> sigunguCodes = regionRepository.findDistinctSigunguCodes();
        List<String> dealYmds = recentYearMonths(properties.getMonthsToCollect());

        log.info("[RentSync] 동기화 시작 - 시군구 {}개 x {}개월", sigunguCodes.size(), dealYmds.size());

        int totalSaved = 0;
        for (String lawdCd : sigunguCodes) {
            totalSaved += rentSigunguSyncService.syncSigungu(lawdCd, dealYmds);
        }

        long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
        log.info("[RentSync] 동기화 완료 - 총 {}건 저장 (소요 {}초)", totalSaved, elapsedSec);
        return totalSaved;
    }

    // 현재 월부터 과거로 N개월치 "yyyyMM" 문자열 목록 생성 (최신순)
    private List<String> recentYearMonths(int months) {
        YearMonth now = YearMonth.now();
        return IntStream.range(0, months)
                .mapToObj(i -> now.minusMonths(i).format(YM_FORMAT))
                .toList();
    }
}
