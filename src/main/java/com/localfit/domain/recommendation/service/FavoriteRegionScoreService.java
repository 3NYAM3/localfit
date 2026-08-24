package com.localfit.domain.recommendation.service;

import com.localfit.domain.recommendation.dto.FavoriteRegionScoreResponse;
import com.localfit.domain.recommendation.dto.RankingInfo;
import com.localfit.domain.recommendation.dto.RentType;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.rent.config.RentSyncProperties;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.domain.subway.dto.SubwayCountProjection;
import com.localfit.domain.subway.repository.SubwayStationRepository;
import com.localfit.domain.user.entity.FavoriteRegion;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRegionScoreService {

    private static final  long MIN_TRANSACTION_COUNT = 5; // 표본이 적어 평균이 왜곡되는 지역을 통계에서 제외하기위한 최소 거래 건수

    public static final String CACHE_NAME = "favoriteScores";

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final RentTransactionRepository rentTransactionRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final RentSyncProperties rentSyncProperties;


    @Cacheable(value = CACHE_NAME, key = "#userId + '::' + #rentType")
    @Transactional(readOnly = true)
    public List<FavoriteRegionScoreResponse> getScores(Long userId, RentType rentType){
        log.info("[FavoriteRegionScore] 캐시 미스 - DB에서 재계산 (userId={}, rentType={})", userId, rentType);
        List<FavoriteRegion> favorites = favoriteRegionRepository.findAllByUserId(userId);

        YearMonth now = YearMonth.now();
        LocalDate start = now.minusMonths(rentSyncProperties.getMonthsToCollect() - 1L).atDay(1);
        LocalDate end = now.atEndOfMonth();

        List<RegionRentStatsProjection> capitalRentStats = rentTransactionRepository.findAllRegionStats(start, end, MIN_TRANSACTION_COUNT);
        List<SubwayCountProjection> capitalSubwayStats = subwayStationRepository.countAllBySigungu();

        return favorites.stream()
                .map(favorite -> toScoreResponse(favorite, rentType, start, end, capitalRentStats, capitalSubwayStats))
                .toList();
    }

    // 관심지역 한건에 대하여 여러 지표의 점수를 각각 계산해 하나의 응답으로 묶는다
    private FavoriteRegionScoreResponse toScoreResponse(FavoriteRegion favorite, RentType rentType, LocalDate start, LocalDate end,
                                                        List<RegionRentStatsProjection> capitalRentStats,
                                                        List<SubwayCountProjection> capitalSubwayStats) {
        Region region = favorite.getRegion();

        FavoriteRegionScoreResponse.HousingScore housing =
                buildHousingScore(region, rentType, start, end, capitalRentStats);
        FavoriteRegionScoreResponse.SubwayScore subway =
                buildSubwayScore(region, capitalSubwayStats);

        return new FavoriteRegionScoreResponse(
                region.getId(), favorite.getPriority(),region.getSido(), region.getSigungu(), region.getDong(),
                housing, subway
        );
    }

    //===============주거지 지표===============
    private FavoriteRegionScoreResponse.HousingScore buildHousingScore(
            Region region, RentType rentType, LocalDate start, LocalDate end,
            List<RegionRentStatsProjection> capitalStats) {

        List<RegionRentStatsProjection> sigunguStats = rentTransactionRepository
                .findRegionStatsBySigungu(region.getSido(), region.getSigungu(), start, end, MIN_TRANSACTION_COUNT);
        List<RegionRentStatsProjection> sidoStats = rentTransactionRepository
                .findRegionStatsBySido(region.getSido(), start, end, MIN_TRANSACTION_COUNT);

        Double amount = extractRentAmount(sigunguStats, region.getId(), rentType);

        // 주거비는 "낮을수록 좋음" -> ascending = true
        RankingInfo sigunguRanking = calculateRanking(
                toAmountMap(sigunguStats, rentType), region.getId(), region.getSigungu(), true);
        RankingInfo sidoRanking = calculateRanking(
                toAmountMap(sidoStats, rentType), region.getId(), region.getSido(), true);
        RankingInfo capitalRanking = calculateRanking(
                toAmountMap(capitalStats, rentType), region.getId(), "수도권 전체", true);

        //표본 거래 건수
        long transactionCount = sigunguStats.stream()
                .filter(s -> s.getRegionId().equals(region.getId()))
                .findFirst()
                .map(RegionRentStatsProjection::getTotalCount)
                .orElse(0L);

        return new FavoriteRegionScoreResponse.HousingScore(
                amount == null ? null : Math.round(amount),
                transactionCount, sigunguRanking, sidoRanking, capitalRanking
        );
    }

    //통계 목록을 <지역id, 금액>으로 변환
    private Map<Long, Double> toAmountMap(List<RegionRentStatsProjection> stats, RentType rentType) {
        return stats.stream()
                .filter(s -> extractRentAmount(s, rentType) != null)
                .collect(Collectors.toMap(RegionRentStatsProjection::getRegionId, s -> extractRentAmount(s, rentType)));
    }

    //특정 지역 하나의 금액
    private Double extractRentAmount(List<RegionRentStatsProjection> stats, Long regionId, RentType rentType) {
        return stats.stream()
                .filter(s -> s.getRegionId().equals(regionId))
                .findFirst()
                .map(s -> extractRentAmount(s, rentType))
                .orElse(null);
    }

    //월세 전세 구분하여 필드 선택
    private Double extractRentAmount(RegionRentStatsProjection stat, RentType rentType) {
        return rentType == RentType.JEONSE ? stat.getJeonseAvgDeposit() : stat.getMonthlyAvgRent();
    }


    //===============지하철 지표===============
    private FavoriteRegionScoreResponse.SubwayScore buildSubwayScore(
            Region region, List<SubwayCountProjection> capitalStats) {

        List<SubwayCountProjection> sidoStats =
                subwayStationRepository.countBySido(region.getSido());

        // 이 지역(시군구)의 역 개수를 시도 통계에서 직접 찾음
        long stationCount = sidoStats.stream()
                .filter(s -> s.getSigungu().equals(region.getSigungu()))
                .findFirst()
                .map(SubwayCountProjection::getStationCount)
                .orElse(0L);

        // 지하철역은 "많을수록 좋음" -> ascending=false
        RankingInfo sidoRanking = calculateSubwayRanking(sidoStats, region.getSigungu(), region.getSido());
        RankingInfo capitalRanking = calculateSubwayRanking(capitalStats, region.getSido() + "|" + region.getSigungu(), "수도권 전체");

        return new FavoriteRegionScoreResponse.SubwayScore(stationCount, sidoRanking, capitalRanking);
    }

    //지하철 통계 전용 순위계산
    private RankingInfo calculateSubwayRanking(List<SubwayCountProjection> stats,
                                               String targetKey, String scopeName) {
        boolean isCapitalScope = targetKey.contains("|");

        Map<String, Double> countByKey = stats.stream()
                .collect(Collectors.toMap(
                        s -> isCapitalScope ? s.getSido() + "|" + s.getSigungu() : s.getSigungu(),
                        s -> s.getStationCount().doubleValue(),
                        (a, b) -> a
                ));

        Double myValue = countByKey.get(targetKey);
        if (myValue == null || countByKey.size() < 2) {
            return null;
        }

        List<Double> sorted = countByKey.values().stream()
                .sorted((a, b) -> Double.compare(b, a)) // 내림차순 - 많을수록 1등
                .toList();

        int rank = sorted.indexOf(myValue) + 1;
        int totalCount = sorted.size();
        double percentile = (double) rank / totalCount * 100;

        return new RankingInfo(scopeName, totalCount, rank, percentile);
    }


    //===============공통 순위 계산===============
    private RankingInfo calculateRanking(Map<Long, Double> valueByRegion, Long regionId,
                                         String scopeName, boolean ascending) {
        Double myValue = valueByRegion.get(regionId);
        if (myValue == null || valueByRegion.size() < 2) {
            return null;
        }

        List<Double> sorted = valueByRegion.values().stream()
                .sorted(ascending ? Double::compareTo : (a, b) -> Double.compare(b, a))
                .toList();

        int rank = sorted.indexOf(myValue) + 1;
        int totalCount = sorted.size();
        double percentile = (double) rank / totalCount * 100;

        return new RankingInfo(scopeName, totalCount, rank, percentile);
    }
}
