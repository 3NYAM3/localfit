package com.localfit.domain.rent.repository;

import com.localfit.domain.region.entity.Region;
import com.localfit.domain.region.repository.RegionRepository;
import com.localfit.domain.rent.dto.RegionRentStatsProjection;
import com.localfit.domain.rent.dto.RentStatsProjection;
import com.localfit.domain.rent.entity.RentTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * RentTransactionRepository 통합테스트
 */
@DataJpaTest
@ActiveProfiles("test")
public class RentTransactionRepositoryTest {
    @Autowired
    private RentTransactionRepository rentTransactionRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Region songpa;      // 서울특별시 송파구 잠실동
    private Region nowon;       // 서울특별시 노원구 상계동
    private Region yeonsu;      // 인천광역시 연수구 송도동

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        songpa = regionRepository.save(Region.builder()
                .regionCode("1171010100").sido("서울특별시").sigungu("송파구").dong("잠실동").build());
        nowon = regionRepository.save(Region.builder()
                .regionCode("1135010100").sido("서울특별시").sigungu("노원구").dong("상계동").build());
        yeonsu = regionRepository.save(Region.builder()
                .regionCode("2818510100").sido("인천광역시").sigungu("연수구").dong("송도동").build());
    }

    // ==================== 단일 지역 통계 ====================

    @Nested
    @DisplayName("특정 지역 통계 조회")
    class FindStatsByRegion {

        @Test
        @DisplayName("전세와 월세를 분리해 각각의 건수와 평균을 집계한다")
        void separatesJeonseAndMonthly() {
            // given - 전세 2건(월세=0), 월세 2건(월세>0)
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 2), 70000L, 0L);
            saveTransaction(songpa, "잠실아파트", 59.0, LocalDate.of(2026, 3, 3), 10000L, 100L);
            saveTransaction(songpa, "잠실아파트", 59.0, LocalDate.of(2026, 3, 4), 20000L, 200L);

            // when
            RentStatsProjection stats = rentTransactionRepository
                    .findStatsByRegion(songpa.getId(), START, END);

            // then
            assertThat(stats.getJeonseCount()).isEqualTo(2L);
            assertThat(stats.getJeonseAvgDeposit()).isEqualTo(60000.0);   // (50000+70000)/2
            assertThat(stats.getMonthlyCount()).isEqualTo(2L);
            assertThat(stats.getMonthlyAvgDeposit()).isEqualTo(15000.0);  // (10000+20000)/2
            assertThat(stats.getMonthlyAvgRent()).isEqualTo(150.0);       // (100+200)/2
        }

        @Test
        @DisplayName("조회 기간을 벗어난 거래는 집계에서 제외된다")
        void excludesOutOfPeriod() {
            // given - 기간 내 1건, 기간 외 1건
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2025, 12, 1), 90000L, 0L);

            // when
            RentStatsProjection stats = rentTransactionRepository
                    .findStatsByRegion(songpa.getId(), START, END);

            // then - 기간 내 1건만 집계
            assertThat(stats.getJeonseCount()).isEqualTo(1L);
            assertThat(stats.getJeonseAvgDeposit()).isEqualTo(50000.0);
        }
    }

    // ==================== 계층별 통계 ====================

    @Nested
    @DisplayName("계층별 지역 통계 조회")
    class FindRegionStats {

        @Test
        @DisplayName("최소 거래 건수 미만인 지역은 집계에서 제외된다")
        void filtersOutBelowMinCount() {
            // given - 송파구 3건, 노원구 1건 (minCount=2로 조회)
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 2), 60000L, 0L);
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 3), 70000L, 0L);
            saveTransaction(nowon, "상계아파트", 59.0, LocalDate.of(2026, 3, 1), 30000L, 0L);

            // when
            List<RegionRentStatsProjection> stats = rentTransactionRepository
                    .findAllRegionStats(START, END, 2L);

            // then - 표본이 부족한 노원구는 제외
            assertThat(stats).hasSize(1);
            assertThat(stats.get(0).getRegionId()).isEqualTo(songpa.getId());
        }

        @Test
        @DisplayName("시도로 범위를 제한하면 해당 시도의 지역만 집계된다")
        void filtersBySido() {
            // given - 서울 2개 지역, 인천 1개 지역
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(nowon, "상계아파트", 59.0, LocalDate.of(2026, 3, 1), 30000L, 0L);
            saveTransaction(yeonsu, "송도아파트", 84.0, LocalDate.of(2026, 3, 1), 40000L, 0L);

            // when
            List<RegionRentStatsProjection> stats = rentTransactionRepository
                    .findRegionStatsBySido("서울특별시", START, END, 1L);

            // then - 인천 연수구는 제외
            assertThat(stats).hasSize(2);
            assertThat(stats).extracting(RegionRentStatsProjection::getRegionId)
                    .containsExactlyInAnyOrder(songpa.getId(), nowon.getId());
        }

        @Test
        @DisplayName("시군구로 범위를 제한하면 해당 시군구의 지역만 집계된다")
        void filtersBySigungu() {
            // given
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(nowon, "상계아파트", 59.0, LocalDate.of(2026, 3, 1), 30000L, 0L);

            // when
            List<RegionRentStatsProjection> stats = rentTransactionRepository
                    .findRegionStatsBySigungu("서울특별시", "송파구", START, END, 1L);

            // then
            assertThat(stats).hasSize(1);
            assertThat(stats.get(0).getRegionId()).isEqualTo(songpa.getId());
        }

        @Test
        @DisplayName("전세만 있는 지역은 월세 평균이 null로 집계된다")
        void jeonseOnly_monthlyRentIsNull() {
            // given - 전세만 2건
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 2), 60000L, 0L);

            // when
            List<RegionRentStatsProjection> stats = rentTransactionRepository
                    .findAllRegionStats(START, END, 1L);

            // then - CASE 조건에 맞는 행이 없으면 AVG는 null
            assertThat(stats.get(0).getJeonseAvgDeposit()).isEqualTo(55000.0);
            assertThat(stats.get(0).getMonthlyAvgRent()).isNull();
        }
    }

    // ==================== 중복 판별 키 ====================

    @Nested
    @DisplayName("중복 판별 키 조회")
    class FindDuplicateKeys {

        @Test
        @DisplayName("시군구코드와 기간에 해당하는 거래의 복합키를 반환한다")
        void returnsCompositeKeys() {
            // given
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);

            // when - 송파구 코드는 11710
            List<String> keys = rentTransactionRepository
                    .findDuplicateKeys("11710", START, END);

            // then - regionId|aptName|area|dealDate|deposit 형식
            assertThat(keys).hasSize(1);
            assertThat(keys.get(0)).contains("잠실아파트", "84.0", "2026-03-01", "50000");
        }

        @Test
        @DisplayName("다른 시군구의 거래는 조회되지 않는다")
        void excludesOtherSigungu() {
            // given - 송파구(11710), 노원구(11350)
            saveTransaction(songpa, "잠실아파트", 84.0, LocalDate.of(2026, 3, 1), 50000L, 0L);
            saveTransaction(nowon, "상계아파트", 59.0, LocalDate.of(2026, 3, 1), 30000L, 0L);

            // when
            List<String> keys = rentTransactionRepository
                    .findDuplicateKeys("11710", START, END);

            // then
            assertThat(keys).hasSize(1);
            assertThat(keys.get(0)).contains("잠실아파트");
        }
    }

    // ==================== 테스트 픽스처 ====================

    /** 전월세 거래 1건 저장 */
    private void saveTransaction(Region region, String aptName, Double area,
                                 LocalDate dealDate, Long deposit, Long monthlyRent) {
        rentTransactionRepository.save(RentTransaction.builder()
                .region(region)
                .aptName(aptName)
                .excluUseArea(area)
                .dealDate(dealDate)
                .deposit(deposit)
                .monthlyRent(monthlyRent)
                .floor(10)
                .buildYear(2020)
                .build());
    }
}
