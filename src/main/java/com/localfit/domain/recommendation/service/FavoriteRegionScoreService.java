package com.localfit.domain.recommendation.service;

import com.localfit.domain.recommendation.dto.FavoriteRegionScoreResponse;
import com.localfit.domain.recommendation.dto.RankingInfo;
import com.localfit.domain.recommendation.dto.RentType;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.rent.config.RentSyncProperties;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.domain.user.entity.FavoriteRegion;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRegionScoreService {

    private static final  long MIN_TRANSACTION_COUNT = 5;
    public static final String CACHE_NAME = "favoriteScores";

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final RentTransactionRepository rentTransactionRepository;
    private final RentSyncProperties rentSyncProperties;

    @Cacheable(value = CACHE_NAME, key = "#userId + '::' + #rentType")
    @Transactional(readOnly = true)
    public List<FavoriteRegionScoreResponse> getScores(Long userId, RentType rentType){
        log.info("[FavoriteRegionScore] 캐시 미스 - DB에서 재계산 (userId={}, rentType={})", userId, rentType);
        List<FavoriteRegion> favorites = favoriteRegionRepository.findAllByUserId(userId);

        YearMonth now = YearMonth.now();
        LocalDate start = now.minusMonths(rentSyncProperties.getMonthsToCollect() - 1L).atDay(1);
        LocalDate end = now.atEndOfMonth();

        List<RegionRentStatsProjection> capitalStats = rentTransactionRepository
                .findAllRegionStats(start, end, MIN_TRANSACTION_COUNT);

        return favorites.stream()
                .map(favorite -> toScoreResponse(favorite, rentType, start, end, capitalStats))
                .toList();
    }

    private FavoriteRegionScoreResponse toScoreResponse(FavoriteRegion favorite, RentType rentType,
                                                        LocalDate start, LocalDate end, List<RegionRentStatsProjection> capitalStats) {
        Region region = favorite.getRegion();

        List<RegionRentStatsProjection> sigunguStats = rentTransactionRepository
                .findRegionStatsBySigungu(region.getSido(), region.getSigungu(), start, end, MIN_TRANSACTION_COUNT);
        List<RegionRentStatsProjection> sidoStats = rentTransactionRepository
                .findRegionStatsBySido(region.getSido(), start, end, MIN_TRANSACTION_COUNT);

        Double amount = extractAmount(sigunguStats, region.getId(), rentType);   // 기준 금액은 시군구 통계에서 추출

        RankingInfo sigunguRanking = calculateRanking(sigunguStats, region.getId(), rentType, region.getSigungu());
        RankingInfo sidoRanking = calculateRanking(sidoStats, region.getId(), rentType, region.getSido());
        RankingInfo capitalRanking = calculateRanking(capitalStats, region.getId(), rentType, "수도권 전체");

        long transactionCount = sigunguStats.stream()
                .filter(s -> s.getRegionId().equals(region.getId()))
                .findFirst()
                .map(RegionRentStatsProjection::getTotalCount)
                .orElse(0L);

        return new FavoriteRegionScoreResponse(
                region.getId(),
                favorite.getPriority(),
                region.getSido(),
                region.getSigungu(),
                region.getDong(),
                amount == null ? null : Math.round(amount),
                transactionCount,
                sigunguRanking,
                sidoRanking,
                capitalRanking
        );
    }

    //주어진 범위 안에서 해당 지역의 백분위를 계산
    private RankingInfo calculateRanking(List<RegionRentStatsProjection> stats, Long regionId,
                                         RentType rentType, String scopeName) {
        // regionId -> 기준 금액 맵으로 변환 (해당 유형 데이터 없는 지역은 제외)
        Map<Long, Double> amountByRegion = stats.stream()
                .filter(s -> extractAmount(s, rentType) != null)
                .collect(java.util.stream.Collectors.toMap(
                        RegionRentStatsProjection::getRegionId,
                        s -> extractAmount(s, rentType)
                ));

        Double myAmount = amountByRegion.get(regionId);
        if (myAmount == null || amountByRegion.size() < 2) {
            return null;   // 비교할 데이터가 부족하면 순위 계산 불가
        }

        // 금액 오름차순 정렬 - 가장 저렴한 지역이 1등
        List<Double> sortedAmounts = amountByRegion.values().stream()
                .sorted()
                .toList();

        int rank = sortedAmounts.indexOf(myAmount) + 1;
        int totalCount = sortedAmounts.size();
        double percentile = (double) rank / totalCount * 100;

        return new RankingInfo(scopeName, totalCount, rank, percentile);
    }

    private Double extractAmount(List<RegionRentStatsProjection> stats, Long regionId, RentType rentType) {
        return stats.stream()
                .filter(s -> s.getRegionId().equals(regionId))
                .findFirst()
                .map(s -> extractAmount(s, rentType))
                .orElse(null);
    }

    private Double extractAmount(RegionRentStatsProjection stat, RentType rentType) {
        return rentType == RentType.JEONSE ? stat.getJeonseAvgDeposit() : stat.getMonthlyAvgRent();
    }
}
