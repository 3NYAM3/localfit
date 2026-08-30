package com.localfit.domain.recommendation.service;

import com.localfit.domain.hospital.dto.HospitalCountProjection;
import com.localfit.domain.hospital.repository.HospitalRepository;
import com.localfit.domain.recommendation.dto.*;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.rent.config.RentSyncProperties;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import com.localfit.domain.subway.dto.SubwayCountProjection;
import com.localfit.domain.subway.repository.SubwayStationRepository;
import com.localfit.domain.user.entity.FavoriteRegion;
import com.localfit.domain.user.entity.ImportanceLevel;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자가 등록한 관심지역들을 계층적으로 평가하는 서비스
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRegionScoreService {

    private static final long MIN_TRANSACTION_COUNT = 5; // 표본이 적어 평균이 왜곡되는 지역을 통계에서 제외하기위한 최소 거래 건수

    private final FavoriteRegionRepository favoriteRegionRepository;
    private final RentTransactionRepository rentTransactionRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final HospitalRepository hospitalRepository;
    private final RentSyncProperties rentSyncProperties;

    /**
     * 사용자의 관심지역 전체에 대해, 지정된 가중치로 계층별 종합점수를 계산
     *
     * @param userId        사용자 ID
     * @param rentType      임대유형
     * @param weightRequest 지표별 중요도
     * @return 관심지역별 계층별 종합점수 목록
     */
    @Transactional(readOnly = true)
    public List<FavoriteRegionScoreResponse> getScores(Long userId, RentType rentType, ScoreWeightRequest weightRequest) {
        List<FavoriteRegion> favoriteRegions = favoriteRegionRepository.findAllByUserId(userId);

        YearMonth now = YearMonth.now();
        LocalDate start = now.minusMonths(rentSyncProperties.getMonthsToCollect() - 1L).atDay(1);
        LocalDate end = now.atEndOfMonth();

        // 수도권 전체 통계는 모든 관심지역에 동일하므로 1회만 조회해 재사용
        List<RegionRentStatsProjection> capitalRentStats = rentTransactionRepository.findAllRegionStats(start, end, MIN_TRANSACTION_COUNT);
        List<SubwayCountProjection> capitalSubwayStats = subwayStationRepository.countAllBySigungu();
        List<HospitalCountProjection> capitalHospitalStats = hospitalRepository.countAllBySigungu();

        Map<IndicatorType, Double> weights = resolveWeights(weightRequest);

        return favoriteRegions.stream()
                .map(favoriteRegion -> toScoreResponse(favoriteRegion, rentType, start, end,
                        capitalRentStats, capitalSubwayStats, capitalHospitalStats, weights))
                .toList();
    }

    /**
     * 관심지역 1건에 대해 계층별 종합 점수를 계산해 하나의 응답으로 만듬
     */
    private FavoriteRegionScoreResponse toScoreResponse(FavoriteRegion favorite, RentType rentType, LocalDate start, LocalDate end,
                                                        List<RegionRentStatsProjection> capitalRentStats,
                                                        List<SubwayCountProjection> capitalSubwayStats,
                                                        List<HospitalCountProjection> capitalHospitalStats,
                                                        Map<IndicatorType, Double> weights) {
        Region region = favorite.getRegion();

        // 주거비 - 시군구/시도는 이 지역 기준으로 조회, 수도권은 미리 조회한 값 재사용
        List<RegionRentStatsProjection> sigunguRentStats = rentTransactionRepository.findRegionStatsBySigungu(region.getSido(), region.getSigungu(), start, end, MIN_TRANSACTION_COUNT);
        List<RegionRentStatsProjection> sidoRentStats = rentTransactionRepository.findRegionStatsBySido(region.getSido(), start, end, MIN_TRANSACTION_COUNT);

        RankingInfo housingCapital = calculateHousingRanking(toAmountMap(capitalRentStats, rentType), region.getId(), "수도권 전체");
        RankingInfo housingSido = calculateHousingRanking(toAmountMap(sidoRentStats, rentType), region.getId(), region.getSido());
        RankingInfo housingSigungu = calculateHousingRanking(toAmountMap(sigunguRentStats, rentType), region.getId(), region.getSigungu());

        // 지하철 - 시도/수도권만 원본 계산, 시군구는 시도 값을 상속
        List<SubwayCountProjection> sidoSubwayStats = subwayStationRepository.countBySido(region.getSido());

        RankingInfo subwayCapital = calculateFacilityRanking(toSubwayCountMap(capitalSubwayStats), region.getSido() + "|" + region.getSigungu(), "수도권 전체");
        RankingInfo subwaySido = calculateFacilityRanking(toSubwayCountMap(sidoSubwayStats), region.getSido() + "|" + region.getSigungu(), region.getSido());
        RankingInfo subwaySigungu = inheritAsSigunguRanking(subwaySido, region.getSigungu());

        // 병원 - 지하철과 동일한 패턴 (시군구 단위 매칭이라 시도값 상속)
        List<HospitalCountProjection> sidoHospitalStats = hospitalRepository.countBySido(region.getSido());

        RankingInfo hospitalCapital = calculateFacilityRanking(toHospitalCountMap(capitalHospitalStats), region.getSido() + "|" + region.getSigungu(), "수도권 전체");
        RankingInfo hospitalSido = calculateFacilityRanking(toHospitalCountMap(sidoHospitalStats), region.getSido() + "|" + region.getSigungu(), region.getSido());
        RankingInfo hospitalSigungu = inheritAsSigunguRanking(hospitalSido, region.getSigungu());

        // 계층별로 지표를 Map으로 모아서 종합 점수 계산
        Map<IndicatorType, RankingInfo> sigunguRankings = new EnumMap<>(IndicatorType.class);
        sigunguRankings.put(IndicatorType.HOUSING, housingSigungu);
        sigunguRankings.put(IndicatorType.SUBWAY, subwaySigungu);
        sigunguRankings.put(IndicatorType.HOSPITAL, hospitalSigungu);

        Map<IndicatorType, RankingInfo> sidoRankings = new EnumMap<>(IndicatorType.class);
        sidoRankings.put(IndicatorType.HOUSING, housingSido);
        sidoRankings.put(IndicatorType.SUBWAY, subwaySido);
        sidoRankings.put(IndicatorType.HOSPITAL, hospitalSido);

        Map<IndicatorType, RankingInfo> capitalRankings = new EnumMap<>(IndicatorType.class);
        capitalRankings.put(IndicatorType.HOUSING, housingCapital);
        capitalRankings.put(IndicatorType.SUBWAY, subwayCapital);
        capitalRankings.put(IndicatorType.HOSPITAL, hospitalCapital);

        return new FavoriteRegionScoreResponse(
                region.getId(), favorite.getPriority(), region.getSido(), region.getSigungu(), region.getDong(),
                new FavoriteRegionScoreResponse.WeightInfo(weights),
                buildTierScore(sigunguRankings, weights),
                buildTierScore(sidoRankings, weights),
                buildTierScore(capitalRankings, weights)
        );
    }

    // ==================== 가중치 계산 ====================

    /**
     * 사용자가 선택한 중요도 등급을 비율로 환산
     * 모든 지표가 NOT_IMPORTANT일 경우 동일 가중치로 계산
     */
    private Map<IndicatorType, Double> resolveWeights(ScoreWeightRequest request) {
        Map<IndicatorType, Integer> rawWeights = Arrays.stream(IndicatorType.values())
                .collect(Collectors.toMap(type -> type, type -> request.get(type).getWeight()));

        int total = rawWeights.values().stream().mapToInt(Integer::intValue).sum();

        if (total == 0) {
            double equalShare = 1.0 / IndicatorType.values().length;
            return rawWeights.keySet().stream()
                    .collect(Collectors.toMap(type -> type, type -> equalShare));
        }

        return rawWeights.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / (double) total));
    }

    // ==================== 계층별 종합 점수 ====================

    /**
     * 하나의 계층에서 각 지표의 순위를 가중치로 합산해 종합점수를 계산
     */
    private FavoriteRegionScoreResponse.TierScore buildTierScore(Map<IndicatorType, RankingInfo> rankings, Map<IndicatorType, Double> weights) {
        double totalScore = 0;
        for (IndicatorType type : IndicatorType.values()) {
            RankingInfo ranking = rankings.get(type);
            double score = ranking == null ? 0 : 100 - ranking.getPercentile();
            totalScore += score * weights.getOrDefault(type, 0.0);
        }

        return new FavoriteRegionScoreResponse.TierScore(
                totalScore,
                rankings.get(IndicatorType.HOUSING),
                rankings.get(IndicatorType.SUBWAY),
                rankings.get(IndicatorType.HOSPITAL));
    }

    // ==================== 주거비 지표 ====================

    /** 통계 목록을 "지역ID -> 금액" 맵으로 변환한다. 순위 계산 시 반복 조회를 피하기 위함 */
    private Map<Long, Double> toAmountMap(List<RegionRentStatsProjection> stats, RentType rentType) {
        return stats.stream()
                .filter(s -> extractRentAmount(s, rentType) != null)
                .collect(Collectors.toMap(RegionRentStatsProjection::getRegionId, s -> extractRentAmount(s, rentType)));
    }

    /** rentType에 따라 전세 보증금 또는 월세액 중 어느 필드를 볼지 결정 */
    private Double extractRentAmount(RegionRentStatsProjection stat, RentType rentType) {
        return rentType == RentType.JEONSE ? stat.getJeonseAvgDeposit() : stat.getMonthlyAvgRent();
    }

    /** 값 목록 안에서 특정지역의 주거비 순위를 계산 */
    private RankingInfo calculateHousingRanking(Map<Long, Double> valueByRegion, Long regionId, String scopeName) {
        Double myValue = valueByRegion.get(regionId);
        if (myValue == null || valueByRegion.size() < 2) {
            return null;
        }

        List<Double> sorted = valueByRegion.values().stream()
                .sorted(Double::compareTo)
                .toList();

        int rank = sorted.indexOf(myValue) + 1;
        int totalCount = sorted.size();
        double percentile = (double) rank / totalCount * 100;

        return new RankingInfo(scopeName, totalCount, rank, percentile);
    }

    // ==================== 지하철/병원 지표 ====================

    /**
     * 지하철/병원 처럼 많을수록 좋음 + 시군구 단위 매칭인 지표의 순위를 계산
     */
    private RankingInfo calculateFacilityRanking(Map<String, Double> countByKey, String targetKey, String scopeName) {
        Double myValue = countByKey.get(targetKey);
        if (myValue == null || countByKey.size() < 2) {
            return null;
        }

        List<Double> sorted = countByKey.values().stream()
                .sorted((a, b) -> Double.compare(b, a))   // 내림차순 - 많을수록 1등
                .toList();

        int rank = sorted.indexOf(myValue) + 1;
        int totalCount = sorted.size();
        double percentile = (double) rank / totalCount * 100;

        return new RankingInfo(scopeName, totalCount, rank, percentile);
    }

    /**
     * 지하철 시도 순위를 시군구 계층에 그대로 상속
     * 원본 데이터 단위 정밀도가 시도 값 이상으로 세분화 불가능이라
     */
    private RankingInfo inheritAsSigunguRanking(RankingInfo sidoRanking, String sigunguName) {
        if (sidoRanking == null) {
            return null;
        }
        return new RankingInfo(
                sigunguName, sidoRanking.getTotalCount(), sidoRanking.getRank(),
                sidoRanking.getPercentile(), true
        );
    }

    // ==================== 지하철 지표 ====================

    /** 지하철 통계 목록을 "키 → 역 개수" 맵으로 변환 */
    private Map<String, Double> toSubwayCountMap(List<SubwayCountProjection> stats) {
        return stats.stream()
                .collect(Collectors.toMap(
                        s -> s.getSido() + "|" + s.getSigungu(),
                        s -> s.getStationCount().doubleValue(),
                        (a, b) -> a
                ));
    }

    // ==================== 병원 지표 ====================

    /** 병원 통계 목록을 "키 → 병원 개수" 맵으로 변환 */
    private Map<String, Double> toHospitalCountMap(List<HospitalCountProjection> stats) {
        return stats.stream()
                .collect(Collectors.toMap(
                        s -> s.getSido() + "|" + s.getSigungu(),
                        s -> s.getHospitalCount().doubleValue(),
                        (a, b) -> a
                ));
    }
}
