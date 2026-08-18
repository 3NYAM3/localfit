package com.localfit.domain.recommendation.service;

import com.localfit.domain.recommendation.dto.RecommendationRequest;
import com.localfit.domain.recommendation.dto.RecommendationResponse;
import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.config.RentSyncProperties;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.dto.RentType;
import com.localfit.domain.rent.repository.RentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 지역 추천 점수 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final long MIN_TRANSACTION_COUNT = 10;   // 적은 표본지역 평균 왜곡 방지를 위한 최소 건수

    private final RentTransactionRepository rentTransactionRepository;
    private final RegionRepository regionRepository;
    private final RentSyncProperties rentSyncProperties;
    private final ScoreNormalizer normalizer;

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(RecommendationRequest request) {
        YearMonth now = YearMonth.now();
        LocalDate start = now.minusMonths(rentSyncProperties.getMonthsToCollect() - 1L).atDay(1);
        LocalDate end = now.atEndOfMonth();

        // 전체 지역 통계를 한 번에 조회
        List<RegionRentStatsProjection> allStats =
                rentTransactionRepository.findAllRegionStats(start, end, MIN_TRANSACTION_COUNT);

        // 기준 금액(전세 보증금 or 월세액)이 있는 지역만 후보로 추림
        List<Candidate> candidates = allStats.stream()
                .map(stat -> toCandidate(stat, request.getRentType()))
                .filter(c -> c != null)
                .toList();

        if (candidates.isEmpty()) {
            RecommendationResponse.ScoreContext emptyContext = new RecommendationResponse.ScoreContext(
                    resolveScope(request.getSido()), buildNotice(request.getSido()), 0L, 0L);
            return new RecommendationResponse(request.getRentType(), 0, emptyContext, List.of());
        }

        // Region 정보를 일괄 조회해 Map으로 준비 (건별 조회 방지)
        Map<Long, Region> regionMap = regionRepository.findAllById(
                        candidates.stream().map(Candidate::regionId).toList())
                .stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));

        // 시도 필터 적용
        List<Candidate> filtered = candidates.stream()
                .filter(c -> matchesSido(regionMap.get(c.regionId()), request.getSido()))
                .toList();

        if (filtered.isEmpty()) {
            RecommendationResponse.ScoreContext emptyContext = new RecommendationResponse.ScoreContext(
                    resolveScope(request.getSido()), buildNotice(request.getSido()), 0L, 0L);
            return new RecommendationResponse(request.getRentType(), 0, emptyContext, List.of());
        }

        // 정규화 - 금액이 낮을수록 높은 점수
        List<Double> amounts = filtered.stream().map(Candidate::amount).toList();
        double min = normalizer.min(amounts);
        double max = normalizer.max(amounts);

        // ScoreContext - 이 점수가 어떤 풀 안에서의 상대 점수인지 명시
        RecommendationResponse.ScoreContext scoreContext = new RecommendationResponse.ScoreContext(
                resolveScope(request.getSido()),
                buildNotice(request.getSido()),
                Math.round(min),
                Math.round(max)
        );

        List<RecommendationResponse.RegionScore> results = new ArrayList<>();
        for (Candidate candidate : filtered) {
            Region region = regionMap.get(candidate.regionId());
            if (region == null) continue;

            double housingScore = normalizer.normalizeInverse(candidate.amount(), min, max);

            results.add(new RecommendationResponse.RegionScore(
                    region.getId(),
                    region.getSido(),
                    region.getSigungu(),
                    region.getDong(),
                    candidate.count(),
                    Math.round(candidate.amount()),
                    housingScore,
                    housingScore   // 현재는 지표가 하나뿐이라 최종 점수 = 주거비 점수
            ));
        }

        // 점수 내림차순 정렬 후 상위 N개
        List<RecommendationResponse.RegionScore> topResults = results.stream()
                .sorted(Comparator.comparingDouble(RecommendationResponse.RegionScore::getTotalScore).reversed())
                .limit(request.getLimit())
                .toList();

        return new RecommendationResponse(request.getRentType(), results.size(),scoreContext,topResults);
    }

    //요청의 sido 파라미터에 따라 비교 범위 문자열을 결정한다.
    private String resolveScope(String sido) {
        return (sido == null || sido.isBlank()) ? "수도권 전체 (서울·인천·경기)" : sido;
    }

    //비교 범위에 따라 점수 해석 안내 문구를 생성한다.
    private String buildNotice(String sido) {
        String scope = resolveScope(sido);
        return "이 점수는 절대적인 수치가 아닌 [" + scope + "] 내에서의 상대적인 주거비 점수입니다. " +
                "같은 100점이라도 비교 범위에 따라 의미가 달라질 수 있습니다.";
    }

    //임대 유형에 맞는 기준 금액을 추출한다. 해당 유형의 데이터가 없으면 null 반환
    private Candidate toCandidate(RegionRentStatsProjection stat, RentType rentType) {
        Double amount = (rentType == RentType.JEONSE)
                ? stat.getJeonseAvgDeposit()
                : stat.getMonthlyAvgRent();

        if (amount == null) {
            return null;
        }
        return new Candidate(stat.getRegionId(), stat.getTotalCount(), amount);
    }

    private boolean matchesSido(Region region, String sido) {
        if (region == null) return false;
        return sido == null || sido.isBlank() || sido.equals(region.getSido());
    }

    //점수 계산 중간 단계용 내부 레코드
    private record Candidate(Long regionId, long count, double amount) {}
}
