package com.localfit.domain.hospital.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * HiraRegionNormalizer 단위 테스트
 */
public class HiraRegionNormalizerTest {
    private final HiraRegionNormalizer normalizer = new HiraRegionNormalizer();

    @Nested
    @DisplayName("시도명 정규화")
    class NormalizeSido {

        @ParameterizedTest
        @DisplayName("HIRA 축약 시도명을 우리 Region 표기로 변환한다")
        @CsvSource({
                "서울, 서울특별시",
                "인천, 인천광역시",
                "경기, 경기도"
        })
        void convertToRegionFormat(String hiraSido, String expected) {
            assertThat(normalizer.normalizeSido(hiraSido)).isEqualTo(expected);
        }

        @Test
        @DisplayName("매핑에 없는 시도명은 그대로 반환한다")
        void unknownSido_returnsAsIs() {
            assertThat(normalizer.normalizeSido("부산")).isEqualTo("부산");
        }
    }

    @Nested
    @DisplayName("시군구명 정규화")
    class NormalizeSigungu {

        @Test
        @DisplayName("서울은 표기가 동일해 그대로 반환한다")
        void seoul_returnsAsIs() {
            assertThat(normalizer.normalizeSigungu("서울", "강남구")).isEqualTo("강남구");
            assertThat(normalizer.normalizeSigungu("서울", "송파구")).isEqualTo("송파구");
        }

        @ParameterizedTest
        @DisplayName("인천은 '인천' 접두어를 제거한다")
        @CsvSource({
                "인천미추홀구, 미추홀구",
                "인천연수구, 연수구",
                "인천남동구, 남동구"
        })
        void incheon_removesPrefix(String hiraSigungu, String expected) {
            assertThat(normalizer.normalizeSigungu("인천", hiraSigungu)).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("경기 일반구는 시 단위로 병합한다")
        @CsvSource({
                "부천원미구, 부천시",
                "부천소사구, 부천시",
                "성남분당구, 성남시",
                "수원영통구, 수원시",
                "안양동안구, 안양시",
                "안산상록구, 안산시",
                "고양일산동구, 고양시",
                "용인수지구, 용인시"
        })
        void gyeonggi_mergesDistrictToCity(String hiraSigungu, String expected) {
            assertThat(normalizer.normalizeSigungu("경기", hiraSigungu)).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("화성시 신설구도 시 단위로 병합한다")
        @CsvSource({
                "화성병점구, 화성시",
                "화성만세구, 화성시",
                "화성동탄구, 화성시",
                "화성효행구, 화성시"
        })
        void hwaseongNewDistricts_mergesToCity(String hiraSigungu, String expected) {
            // 행정구역 개편으로 신설된 구. 우리 Region 테이블(법정동코드 기준)에는
            // 아직 없어 매칭 실패했던 케이스
            assertThat(normalizer.normalizeSigungu("경기", hiraSigungu)).isEqualTo(expected);
        }

        @Test
        @DisplayName("경기 일반구가 아닌 시군구는 그대로 반환한다")
        void gyeonggiPlainCity_returnsAsIs() {
            assertThat(normalizer.normalizeSigungu("경기", "김포시")).isEqualTo("김포시");
            assertThat(normalizer.normalizeSigungu("경기", "파주시")).isEqualTo("파주시");
        }

        @Test
        @DisplayName("시군구명이 null이면 null을 반환한다")
        void nullSigungu_returnsNull() {
            assertThat(normalizer.normalizeSigungu("서울", null)).isNull();
        }
    }
}
