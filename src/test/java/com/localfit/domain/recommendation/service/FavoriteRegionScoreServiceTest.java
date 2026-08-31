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
import com.localfit.domain.user.entity.User;
import com.localfit.domain.user.repository.FavoriteRegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * FavoriteRegionScoreService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
public class FavoriteRegionScoreServiceTest {

    @Mock
    private FavoriteRegionRepository favoriteRegionRepository;

    @Mock
    private RentTransactionRepository rentTransactionRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private RentSyncProperties rentSyncProperties;

    @InjectMocks
    private FavoriteRegionScoreService scoreService;


    @Nested
    @DisplayName("가중치 환산")
    class ResolveWeights{

        @Test
        @DisplayName("모든 지표가 NORMAL이면 균등 가중치(각33.3%)로 환산된다")
        void allNormal_equalWeights(){
            //given
            ScoreWeightRequest request = createWeightRequest(
                    ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL);
            stubEmptyStats();

            //when
            List<FavoriteRegionScoreResponse> result = scoreService.getScores(1L, RentType.JEONSE, request);

            //then
            FavoriteRegionScoreResponse.WeightInfo weights = result.getFirst().getWeights();
            assertThat(weights.getHousing()).isEqualTo(33.3);
            assertThat(weights.getSubway()).isEqualTo(33.3);
            assertThat(weights.getHospital()).isEqualTo(33.3);
        }

        @Test
        @DisplayName("중요도가 다르면 비율에 따라 가중치가 배분된다")
        void differentImportance_proportionalWeights() {
            // given - VERY_IMPORTANT(4) : NORMAL(2) : NORMAL(2) = 50% : 25% : 25%
            ScoreWeightRequest request = createWeightRequest(
                    ImportanceLevel.VERY_IMPORTANT, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL);
            stubEmptyStats();

            // when
            List<FavoriteRegionScoreResponse> results =
                    scoreService.getScores(1L, RentType.JEONSE, request);

            // then
            FavoriteRegionScoreResponse.WeightInfo weights = results.getFirst().getWeights();
            assertThat(weights.getHousing()).isEqualTo(50.0);
            assertThat(weights.getSubway()).isEqualTo(25.0);
            assertThat(weights.getHospital()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("모든 지표가 NOT_IMPORTANT(가중치 0)이면 균등 가중치로 대체된다")
        void allNotImportant_fallbackToEqualWeights() {
            // given - 전부 0이면 0으로 나눌 수 없으므로 균등 분배로 대체
            ScoreWeightRequest request = createWeightRequest(
                    ImportanceLevel.NOT_IMPORTANT, ImportanceLevel.NOT_IMPORTANT, ImportanceLevel.NOT_IMPORTANT);
            stubEmptyStats();

            // when
            List<FavoriteRegionScoreResponse> results =
                    scoreService.getScores(1L, RentType.JEONSE, request);

            // then
            FavoriteRegionScoreResponse.WeightInfo weights = results.getFirst().getWeights();
            assertThat(weights.getHousing()).isEqualTo(33.3);
            assertThat(weights.getSubway()).isEqualTo(33.3);
            assertThat(weights.getHospital()).isEqualTo(33.3);
        }
    }

    // ==================== 주거비 순위 ====================

    @Nested
    @DisplayName("주거비 순위 계산")
    class HousingRanking {

        @Test
        @DisplayName("주거비가 가장 저렴한 지역이 1위가 된다")
        void cheapest_rankFirst() {
            // given - 대상 지역(id=1)이 3개 중 가장 저렴
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));

            List<RegionRentStatsProjection> stats = List.of(
                    rentStats(1L, 30000.0),   // 대상 - 가장 저렴
                    rentStats(2L, 50000.0),
                    rentStats(3L, 70000.0)
            );
            given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                    .willReturn(stats);
            given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                    .willReturn(stats);
            given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                    .willReturn(stats);
            stubEmptyFacilityStats();

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then
            RankingInfo ranking = results.getFirst().getSigunguTier().getHousingRanking();
            assertThat(ranking.getRank()).isEqualTo(1);
            assertThat(ranking.getTotalCount()).isEqualTo(3);
            assertThat(ranking.isInherited()).isFalse();
        }

        @Test
        @DisplayName("비교 대상이 1개뿐이면 순위를 계산하지 않고 null을 반환한다")
        void singleTarget_returnsNull() {
            // given - 비교 대상이 자기 자신뿐이면 순위가 의미 없음
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));

            List<RegionRentStatsProjection> onlyOne = List.of(rentStats(1L, 30000.0));
            given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                    .willReturn(onlyOne);
            given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                    .willReturn(onlyOne);
            given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                    .willReturn(onlyOne);
            stubEmptyFacilityStats();

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then
            assertThat(results.getFirst().getSigunguTier().getHousingRanking()).isNull();
        }

        @Test
        @DisplayName("월세 조회 시 전세 보증금이 아닌 월세액을 기준으로 순위를 매긴다")
        void monthlyType_usesMonthlyRent() {
            // given - 전세 보증금과 월세액의 순위가 반대가 되도록 설정
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));

            List<RegionRentStatsProjection> stats = List.of(
                    rentStats(1L, 70000.0, 50.0),   // 전세는 비싸지만 월세는 저렴
                    rentStats(2L, 30000.0, 100.0)
            );
            given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                    .willReturn(stats);
            given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                    .willReturn(stats);
            given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                    .willReturn(stats);
            stubEmptyFacilityStats();

            // when - 월세 기준으로 조회
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.MONTHLY, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then - 월세가 저렴하므로 1위
            assertThat(results.getFirst().getSigunguTier().getHousingRanking().getRank()).isEqualTo(1);
        }
    }

    // ==================== 시설 순위 (지하철/병원) ====================

    @Nested
    @DisplayName("시설 순위 계산")
    class FacilityRanking {

        @Test
        @DisplayName("지하철역이 가장 많은 지역이 1위가 된다")
        void mostStations_rankFirst() {
            // given - 대상 지역(송파구)이 역이 가장 많음
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));
            stubEmptyRentStats();

            List<SubwayCountProjection> subwayStats = List.of(
                    subwayCount("서울특별시", "송파구", 30L),   // 대상 - 가장 많음
                    subwayCount("서울특별시", "노원구", 20L),
                    subwayCount("서울특별시", "강북구", 10L)
            );
            given(subwayStationRepository.countBySido(any())).willReturn(subwayStats);
            given(subwayStationRepository.countAllBySigungu()).willReturn(subwayStats);
            given(hospitalRepository.countBySido(any())).willReturn(List.of());
            given(hospitalRepository.countAllBySigungu()).willReturn(List.of());

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then - 많을수록 좋으므로 1위
            RankingInfo ranking = results.getFirst().getSidoTier().getSubwayRanking();
            assertThat(ranking.getRank()).isEqualTo(1);
            assertThat(ranking.getTotalCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("시군구 계층의 시설 순위는 시도 값을 상속하며 inherited가 true다")
        void sigunguTier_inheritsFromSido() {
            // given
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));
            stubEmptyRentStats();

            List<SubwayCountProjection> subwayStats = List.of(
                    subwayCount("서울특별시", "송파구", 30L),
                    subwayCount("서울특별시", "노원구", 20L)
            );
            given(subwayStationRepository.countBySido(any())).willReturn(subwayStats);
            given(subwayStationRepository.countAllBySigungu()).willReturn(subwayStats);
            given(hospitalRepository.countBySido(any())).willReturn(List.of());
            given(hospitalRepository.countAllBySigungu()).willReturn(List.of());

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then - 시군구 계층은 시도 값을 상속 (rank/percentile 동일, scopeName만 시군구명)
            RankingInfo sidoRanking = results.getFirst().getSidoTier().getSubwayRanking();
            RankingInfo sigunguRanking = results.getFirst().getSigunguTier().getSubwayRanking();

            assertThat(sigunguRanking.isInherited()).isTrue();
            assertThat(sigunguRanking.getRank()).isEqualTo(sidoRanking.getRank());
            assertThat(sigunguRanking.getPercentile()).isEqualTo(sidoRanking.getPercentile());
            assertThat(sigunguRanking.getScopeName()).isEqualTo("송파구");
        }

        @Test
        @DisplayName("병원도 지하철과 동일하게 많을수록 상위 순위가 된다")
        void hospital_sameAsSubway() {
            // given
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));
            stubEmptyRentStats();
            given(subwayStationRepository.countBySido(any())).willReturn(List.of());
            given(subwayStationRepository.countAllBySigungu()).willReturn(List.of());

            List<HospitalCountProjection> hospitalStats = List.of(
                    hospitalCount("서울특별시", "송파구", 25L),   // 대상 - 가장 많음
                    hospitalCount("서울특별시", "노원구", 15L),
                    hospitalCount("서울특별시", "강북구", 5L)
            );
            given(hospitalRepository.countBySido(any())).willReturn(hospitalStats);
            given(hospitalRepository.countAllBySigungu()).willReturn(hospitalStats);

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then
            RankingInfo ranking = results.getFirst().getSidoTier().getHospitalRanking();
            assertThat(ranking.getRank()).isEqualTo(1);
            assertThat(ranking.getTotalCount()).isEqualTo(3);
        }
    }

    // ==================== 종합 점수 ====================

    @Nested
    @DisplayName("계층별 종합 점수")
    class TierScore {

        @Test
        @DisplayName("모든 지표가 1위면 종합 점수가 100점에 가깝다")
        void allFirstPlace_highScore() {
            // given - 3개 지표 모두 1위
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));

            List<RegionRentStatsProjection> rentStats = List.of(
                    rentStats(1L, 10000.0),    // 가장 저렴
                    rentStats(2L, 50000.0)
            );
            given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                    .willReturn(rentStats);
            given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                    .willReturn(rentStats);
            given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                    .willReturn(rentStats);

            List<SubwayCountProjection> subwayStats = List.of(
                    subwayCount("서울특별시", "송파구", 30L),
                    subwayCount("서울특별시", "노원구", 10L)
            );
            given(subwayStationRepository.countBySido(any())).willReturn(subwayStats);
            given(subwayStationRepository.countAllBySigungu()).willReturn(subwayStats);

            List<HospitalCountProjection> hospitalStats = List.of(
                    hospitalCount("서울특별시", "송파구", 25L),
                    hospitalCount("서울특별시", "노원구", 5L)
            );
            given(hospitalRepository.countBySido(any())).willReturn(hospitalStats);
            given(hospitalRepository.countAllBySigungu()).willReturn(hospitalStats);

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then - 2개 중 1위 = percentile 50 → 점수 50점씩
            assertThat(results.getFirst().getSidoTier().getTotalScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("순위 정보가 없는 지표는 0점으로 처리된다")
        void nullRanking_treatedAsZero() {
            // given - 주거비만 순위가 있고 시설 지표는 데이터 없음
            given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
            given(favoriteRegionRepository.findAllByUserId(anyLong()))
                    .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));

            List<RegionRentStatsProjection> rentStats = List.of(
                    rentStats(1L, 10000.0),
                    rentStats(2L, 50000.0)
            );
            given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                    .willReturn(rentStats);
            given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                    .willReturn(rentStats);
            given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                    .willReturn(rentStats);
            stubEmptyFacilityStats();

            // when
            List<FavoriteRegionScoreResponse> results = scoreService.getScores(
                    1L, RentType.JEONSE, createWeightRequest(
                            ImportanceLevel.NORMAL, ImportanceLevel.NORMAL, ImportanceLevel.NORMAL));

            // then - 주거비 50점 × 33.3% = 약 16.7점 (시설 지표는 0점)
            assertThat(results.getFirst().getSidoTier().getTotalScore()).isEqualTo(16.7);
        }
    }

    // ==================== 테스트 픽스처 ====================

    /** 관심지역 엔티티 생성. Region의 id는 리플렉션으로 주입 */
    private FavoriteRegion createFavoriteRegion(Long regionId, String sido, String sigungu,
                                                String dong, int priority) {
        Region region = Region.builder()
                .regionCode("1171010100")
                .sido(sido)
                .sigungu(sigungu)
                .dong(dong)
                .build();
        ReflectionTestUtils.setField(region, "id", regionId);

        return FavoriteRegion.builder()
                .user(User.builder().email("test@test.com").password("pw").nickname("tester").build())
                .region(region)
                .priority(priority)
                .build();
    }

    /** 지표별 중요도를 담은 요청 생성 */
    private ScoreWeightRequest createWeightRequest(ImportanceLevel housing, ImportanceLevel subway,
                                                   ImportanceLevel hospital) {
        ScoreWeightRequest request = new ScoreWeightRequest();
        ReflectionTestUtils.setField(request, "importance", Map.of(
                IndicatorType.HOUSING, housing,
                IndicatorType.SUBWAY, subway,
                IndicatorType.HOSPITAL, hospital
        ));
        return request;
    }

    /** 전세 기준 통계 Projection (월세액은 기본값) */
    private RegionRentStatsProjection rentStats(Long regionId, Double jeonseAvgDeposit) {
        return rentStats(regionId, jeonseAvgDeposit, 50.0);
    }

    /** 전세/월세 통계 Projection */
    private RegionRentStatsProjection rentStats(Long regionId, Double jeonseAvgDeposit,
                                                Double monthlyAvgRent) {
        return new RegionRentStatsProjection() {
            @Override public Long getRegionId() { return regionId; }
            @Override public Double getJeonseAvgDeposit() { return jeonseAvgDeposit; }
            @Override public Double getMonthlyAvgRent() { return monthlyAvgRent; }
        };
    }

    /** 지하철 개수 Projection */
    private SubwayCountProjection subwayCount(String sido, String sigungu, Long count) {
        return new SubwayCountProjection() {
            @Override public String getSido() { return sido; }
            @Override public String getSigungu() { return sigungu; }
            @Override public Long getStationCount() { return count; }
        };
    }

    /** 병원 개수 Projection */
    private HospitalCountProjection hospitalCount(String sido, String sigungu, Long count) {
        return new HospitalCountProjection() {
            @Override public String getSido() { return sido; }
            @Override public String getSigungu() { return sigungu; }
            @Override public Long getHospitalCount() { return count; }
        };
    }

    /** 모든 통계를 빈 목록으로 스텁 (가중치 계산만 검증할 때 사용) */
    private void stubEmptyStats() {
        given(rentSyncProperties.getMonthsToCollect()).willReturn(6);
        given(favoriteRegionRepository.findAllByUserId(anyLong()))
                .willReturn(List.of(createFavoriteRegion(1L, "서울특별시", "송파구", "잠실동", 1)));
        stubEmptyRentStats();
        stubEmptyFacilityStats();
    }

    /** 주거비 통계만 빈 목록으로 스텁 */
    private void stubEmptyRentStats() {
        given(rentTransactionRepository.findRegionStatsBySigungu(any(), any(), any(), any(), anyLong()))
                .willReturn(List.of());
        given(rentTransactionRepository.findRegionStatsBySido(any(), any(), any(), anyLong()))
                .willReturn(List.of());
        given(rentTransactionRepository.findAllRegionStats(any(), any(), anyLong()))
                .willReturn(List.of());
    }

    /** 시설 통계만 빈 목록으로 스텁 */
    private void stubEmptyFacilityStats() {
        given(subwayStationRepository.countBySido(any())).willReturn(List.of());
        given(subwayStationRepository.countAllBySigungu()).willReturn(List.of());
        given(hospitalRepository.countBySido(any())).willReturn(List.of());
        given(hospitalRepository.countAllBySigungu()).willReturn(List.of());
    }
}

